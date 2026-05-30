package com.example.musiclog.data.local

import androidx.room.Entity

@Entity(
    tableName = "playlist_music_cross_ref",
    primaryKeys = ["playlistId", "musicId"]
)
data class PlaylistMusicCrossRef(
    val playlistId: String,
    val musicId: String
)