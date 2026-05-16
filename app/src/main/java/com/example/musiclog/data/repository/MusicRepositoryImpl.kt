package com.example.musiclog.data.repository

import com.example.musiclog.data.local.MusicDao
import com.example.musiclog.data.mapper.toDomain
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDao: MusicDao // 추후에 NetworkModule의 YouTube, Firebase 객체도 이곳에 주입해 조립해야함.
) : MusicRepository {

    override fun getAllMusic(): Flow<List<Music>> { // 로컬 엔티티 스트림을 받아 도메인 모델 스트림으로 가공 변환
        return musicDao.getAllMusic().map { entities ->
            entities.map { it.toDomain() } // it = MusicEntity
        }
    }

    override suspend fun searchMusic(query: String): List<Music> {
        // TODO: 원격 YouTube API 연동 검색 비즈니스 로직 적용 예정 단계
        return emptyList()
    }

    override suspend fun incrementPlayCount(musicId: String) {
        musicDao.incrementPlayCount(musicId, System.currentTimeMillis())
    }

    override suspend fun updateAlbumArt(musicId: String, newUri: String) {
        musicDao.updateAlbumArt(musicId, newUri)
    }

    override suspend fun getTopMusic(limit: Int): List<Music> {
        return musicDao.getTopMusic(limit).map { it.toDomain() }
    }
}