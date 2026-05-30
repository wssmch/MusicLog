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

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDao: MusicDao,
    private val youtubeService: YouTube, // 추후에 NetworkModule의 YouTube, Firebase 객체도 이곳에 주입해 조립해야함
    private val firebaseClient: FirebaseClient
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
                firebaseClient.incrementArtistPlayCount(it.artist)
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
}