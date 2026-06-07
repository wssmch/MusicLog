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
    private val youtubeService: YouTube,
    private val firebaseClient: FirebaseClient,
    private val playlistDao: PlaylistDao
) : MusicRepository {

    override fun getAllMusic(): Flow<List<Music>> {
        return musicDao.getAllMusic().map { entities ->
            entities.map { entity ->
                val domain = entity.toDomain()
                // 💡 로컬 DB에서 UI 레이어로 방출되기 직전 전량 일괄 디코딩 래핑 적용
                domain.copy(
                    title = android.text.Html.fromHtml(domain.title, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                    artist = android.text.Html.fromHtml(domain.artist, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                )
            }
        }
    }

    override suspend fun searchMusic(query: String): List<Music> {
        return withContext(Dispatchers.IO) {
            try {
                val request = youtubeService.search().list(listOf("snippet"))
                request.q = query
                request.type = listOf("video")
                request.maxResults = 50L

                val response = request.execute()
                val items = response.items ?: emptyList()

                items.mapNotNull { result ->
                    val videoId = result.id?.videoId ?: return@mapNotNull null
                    val snippet = result.snippet ?: return@mapNotNull null

                    Music(
                        id = videoId,
                        // 💡 특수문자 깨짐 방지를 위해 Html.fromHtml 디코딩 추가
                        title = android.text.Html.fromHtml(snippet.title ?: "제목 없음", android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                        artist = android.text.Html.fromHtml(snippet.channelTitle ?: "알 수 없는 아티스트", android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                        albumArtUrl = snippet.thumbnails?.high?.url ?: "",
                        customAlbumArtUri = null,
                        playCount = 1,
                        lastPlayedTimeStamp = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
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

    override suspend fun incrementPlayCount(musicId: String) {
        withContext(Dispatchers.IO) {
            musicDao.incrementPlayCount(musicId, System.currentTimeMillis())
            val entity = musicDao.getMusicById(musicId)
            entity?.let {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"

                // 💡 누수 방지 절대값 동기화 적용
                val allMusic = musicDao.getAllMusic().first()
                val absoluteArtistCount = allMusic.filter { m -> m.artist == it.artist }.sumOf { m -> m.playCount }

                firebaseClient.syncArtistPlayCount(it.artist, currentUid, absoluteArtistCount)
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val localPlaylists = playlistDao.getPlaylistsWithMusic().first()
            if (localPlaylists.isNotEmpty()) {
                firebaseClient.syncPlaylistsToCloud(uid, localPlaylists)
            }

            val localHistory = musicDao.getAllMusic().first()
            if (localHistory.isNotEmpty()) {
                firebaseClient.syncHistoryToCloud(uid, localHistory)
            }
        }
    }

    override suspend fun restoreCloudDataToLocal() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val cloudHistory = firebaseClient.fetchHistoryFromCloud(uid)
            for (data in cloudHistory) {
                val musicId = data["id"] as? String ?: continue
                val musicEntity = MusicEntity(
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

            val cloudPlaylists = firebaseClient.fetchPlaylistsFromCloud(uid)
            for (data in cloudPlaylists) {
                val playlistId = data["playlistId"] as? String ?: continue

                val playlistEntity = PlaylistEntity(
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

                    val musicEntity = MusicEntity(
                        id = musicId,
                        title = songData["title"] as? String ?: "제목 없음",
                        artist = songData["artist"] as? String ?: "아티스트 미상",
                        albumArtUrl = songData["albumArtUrl"] as? String ?: "",
                        customAlbumArtUri = songData["customAlbumArtUri"] as? String,
                        playCount = (songData["playCount"] as? Long)?.toInt() ?: 0,
                        lastPlayedTimeStamp = songData["lastPlayedTimeStamp"] as? Long ?: System.currentTimeMillis()
                    )
                    musicDao.insertMusic(musicEntity)

                    val crossRef = PlaylistMusicCrossRef(
                        playlistId = playlistId,
                        musicId = musicId
                    )
                    playlistDao.insertPlaylistMusicCrossRef(crossRef)
                }
            }
        }
    }
}