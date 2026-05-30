package com.example.musiclog.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithMusic(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id", // MusicEntity의 기본키
        associateBy = Junction(PlaylistMusicCrossRef::class)
    )
    val songs: List<MusicEntity>
)