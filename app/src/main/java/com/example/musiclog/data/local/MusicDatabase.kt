package com.example.musiclog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    // 새로 추가한 재생목록 관련 엔티티 2개를 entities 배열에 추가.
    entities = [MusicEntity::class, PlaylistEntity::class, PlaylistMusicCrossRef::class],
    version = 2, // 스키마가 변경되었으므로 버전을 올려줌
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    // Hilt가 참조할 수 있도록 PlaylistDao 접근 추상 메서드를 추가.
    abstract fun playlistDao(): PlaylistDao
}