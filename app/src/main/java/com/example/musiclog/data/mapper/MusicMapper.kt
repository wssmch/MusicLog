package com.example.musiclog.data.mapper

import com.example.musiclog.data.local.MusicEntity
import com.example.musiclog.domain.model.Music

/**
 * 데이터 계층의 MusicEntity와 도메인 계층의 Music 간의
 * 상호 변환을 담당하는 아키텍처 확장 함수입니다.
 */
fun MusicEntity.toDomain() : Music { // data패키지의 MusicEntity와 domain패키지의 Music간의
    return Music(                   // 변환을 담당하는 아키텍쳐 확장함수
        id = id,
        title = title,
        artist = artist,
        albumArtUrl = albumArtUrl,
        customAlbumArtUri = customAlbumArtUri,
        playCount = playCount,
        lastPlayedTimeStamp = lastPlayedTimeStamp
    )
}

fun Music.toEntity() : MusicEntity { // 나중에 ui패키지에서 반환하는 순수 데이터를 DB에 저장하기 위해 사용
    return MusicEntity(
        id = id,
        title = title,
        artist = artist,
        albumArtUrl = albumArtUrl,
        customAlbumArtUri = customAlbumArtUri,
        playCount = playCount,
        lastPlayedTimeStamp = lastPlayedTimeStamp
    )
}