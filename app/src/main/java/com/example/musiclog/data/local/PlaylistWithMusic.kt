package com.example.musiclog.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithMusic(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId", // PlaylistEntity의 기본키
        entityColumn = "id",         // MusicEntity의 기본키
        associateBy = Junction(
            value = PlaylistMusicCrossRef::class,
            parentColumn = "playlistId", // PlaylistMusicCrossRef 내의 PlaylistEntity 참조 컬럼명
            entityColumn = "musicId"     // PlaylistMusicCrossRef 내의 MusicEntity 참조 컬럼명
        )
    )
    val songs: List<MusicEntity>
)