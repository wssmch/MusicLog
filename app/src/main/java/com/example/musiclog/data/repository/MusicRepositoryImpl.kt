package com.example.musiclog.data.repository

import android.content.ClipData.newUri
import com.example.musiclog.data.local.MusicDao
import com.example.musiclog.data.mapper.toDomain
import com.example.musiclog.data.mapper.toEntity
import com.example.musiclog.data.remote.FirebaseClient
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.example.musiclog.data.local.PlaylistEntity
import com.example.musiclog.data.local.MusicEntity
import com.example.musiclog.data.local.PlaylistMusicCrossRef
import com.example.musiclog.data.local.PlaylistDao
import kotlinx.coroutines.flow.first

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDao: MusicDao,
    private val youtubeService: YouTube, // 추후에 NetworkModule의 YouTube, Firebase 객체도 이곳에 주입해 조립해야함
    private val firebaseClient: FirebaseClient,
    private val playlistDao: PlaylistDao
) : MusicRepository {

    override fun getAllMusic(): Flow<List<Music>> { // 로컬 엔티티 스트림을 받아 도메인 모델 스트림으로 가공 변환
        return musicDao.getAllMusic().map { entities ->
            entities.map { it.toDomain() } // it = MusicEntity
        }
    }

    override suspend fun searchMusic(query: String): List<Music> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. YouTube API 검색 요청 세팅 (100 유닛 소모)
                val request = youtubeService.search().list(listOf("snippet"))
                request.q = query
                request.type = listOf("video")
                request.maxResults = 10L // 최대 10개만 가져옴

                // 2. 구글 서버에 검색 실행 및 결과 받아오기
                val response = request.execute()
                val items = response.items ?: emptyList()

                // 3. SearchResult(구글 DTO) -> Music(앱 도메인 모델) 변환
                items.mapNotNull { result ->
                    val videoId = result.id?.videoId ?: return@mapNotNull null
                    val snippet = result.snippet ?: return@mapNotNull null

                    Music(
                        id = videoId,
                        title = snippet.title ?: "제목 없음",
                        artist = snippet.channelTitle ?: "알 수 없는 아티스트",
                        albumArtUrl = snippet.thumbnails?.high?.url ?: "",
                        customAlbumArtUri = null,
                        playCount = 1, // 최초 로깅 및 재생 인텐트 호출 시점의 정합성을 위해 1로 초기화
                        lastPlayedTimeStamp = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                // 네트워크 에러나 할당량 초과 시 앱이 죽지 않도록 빈 리스트 반환 (나중에 ViewModel에서 에러 처리)
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun insertMusic(music: Music) {
        withContext(Dispatchers.IO) {
            musicDao.insertMusic(music.toEntity())
        }
    }

    override suspend fun incrementPlayCount(musicId: String) { // 기존 로컬 db카운터만 업데이트하는 로직에서 로컬과 원격을 동기화하도록 수정
        withContext(Dispatchers.IO) {
            musicDao.incrementPlayCount(musicId, System.currentTimeMillis())
            val entity = musicDao.getMusicById(musicId)
            entity?.let {
                val currentUid =
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        ?: "guest_user"
                firebaseClient.incrementArtistPlayCount(it.artist, currentUid)
            }
        }
    }

    override suspend fun updateAlbumArt(musicId: String, newUri: String) {
        withContext(Dispatchers.IO) {
            musicDao.updateAlbumArt(musicId, newUri)
        }
    }

    override suspend fun getTopMusic(limit: Int): List<Music> {
        return withContext(Dispatchers.IO) {
            musicDao.getTopMusic(limit).map { it.toDomain() }
        }
    }

    override suspend fun backupLocalDataToCloud() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // 1. 플레이리스트 데이터 백업
            val localPlaylists = playlistDao.getPlaylistsWithMusic().first()
            if (localPlaylists.isNotEmpty()) {
                firebaseClient.syncPlaylistsToCloud(uid, localPlaylists)
            }

            // 2. 누락되었던 전체 청취 기록(music_table) 백업 파이프라인 추가
            val localHistory = musicDao.getAllMusic().first()
            if (localHistory.isNotEmpty()) {
                firebaseClient.syncHistoryToCloud(uid, localHistory)
            }
        }
    }

    override suspend fun restoreCloudDataToLocal() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // 1. 누락되었던 전체 청취 기록 복원 파이프라인 추가 (가장 먼저 복구하여 기반 데이터 마련)
            val cloudHistory = firebaseClient.fetchHistoryFromCloud(uid)
            for (data in cloudHistory) {
                val musicId = data["id"] as? String ?: continue
                val musicEntity = com.example.musiclog.data.local.MusicEntity(
                    id = musicId,
                    title = data["title"] as? String ?: "제목 없음",
                    artist = data["artist"] as? String ?: "아티스트 미상",
                    albumArtUrl = data["albumArtUrl"] as? String ?: "",
                    customAlbumArtUri = data["customAlbumArtUri"] as? String,
                    playCount = (data["playCount"] as? Long)?.toInt() ?: 0,
                    lastPlayedTimeStamp = data["lastPlayedTimeStamp"] as? Long ?: System.currentTimeMillis()
                )
                musicDao.insertMusic(musicEntity)
            }

            // 2. 플레이리스트 복원 로직 유지
            val cloudPlaylists = firebaseClient.fetchPlaylistsFromCloud(uid)
            for (data in cloudPlaylists) {
                val playlistId = data["playlistId"] as? String ?: continue

                val playlistEntity = com.example.musiclog.data.local.PlaylistEntity(
                    playlistId = playlistId,
                    name = data["name"] as? String ?: "알 수 없는 재생목록",
                    coverUri = data["coverUri"] as? String,
                    isAutoGenerated = data["isAutoGenerated"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
                )
                playlistDao.insertPlaylist(playlistEntity)

                val songs = data["songs"] as? List<Map<String, Any>> ?: emptyList()
                for (songData in songs) {
                    val musicId = songData["id"] as? String ?: continue

                    // 플레이리스트에만 존재하고 일반 history에 없는 예외 곡 데이터를 위한 방어적 인서트
                    val musicEntity = com.example.musiclog.data.local.MusicEntity(
                        id = musicId,
                        title = songData["title"] as? String ?: "제목 없음",
                        artist = songData["artist"] as? String ?: "아티스트 미상",
                        albumArtUrl = songData["albumArtUrl"] as? String ?: "",
                        customAlbumArtUri = songData["customAlbumArtUri"] as? String,
                        playCount = (songData["playCount"] as? Long)?.toInt() ?: 0,
                        lastPlayedTimeStamp = songData["lastPlayedTimeStamp"] as? Long ?: System.currentTimeMillis()
                    )
                    musicDao.insertMusic(musicEntity)

                    val crossRef = com.example.musiclog.data.local.PlaylistMusicCrossRef(
                        playlistId = playlistId,
                        musicId = musicId
                    )
                    playlistDao.insertPlaylistMusicCrossRef(crossRef)
                }
            }
        }
    }
}