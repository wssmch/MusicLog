package com.example.musiclog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

@Entity(tableName = "music_table")
data class MusicEntity(
    @PrimaryKey val id : String, // 각 데이터를 구분하기 위해 id를 사용

    val title : String,
    val artist : String,
    val albumArtUrl : String,
    val customAlbumArtUri : String?,
    val playCount : Int = 0,
    val lastPlayedTimeStamp: Long = System.currentTimeMillis()

)