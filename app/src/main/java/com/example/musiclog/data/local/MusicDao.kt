package com.example.musiclog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao{
    @Query("SELECT * FROM music_table")
    fun getAllMusic() : Flow<List<MusicEntity>> // Flow를 반환하면 DB 값이 바뀔 때마다 ui에 새 리스트를 자동으로 보냄.

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 만약 같은 ID의 노래가 이미 있다면 REPLACE를 수행.
    suspend fun insertMusic(music : MusicEntity)

    @Query("UPDATE music_table SET playCount = playCount + 1, lastPlayedTimeStamp = :timestamp WHERE id = :musicId")
    suspend fun incrementPlayCount(musicId : String, timestamp : Long) // 재생횟수를 1만큼 증가. :musicId처럼 콜론을 붙이면 함수의 파라미터 값을
                                                                       // SQL에 대입함.

    @Query("UPDATE music_table SET customAlbumArtUri = :newUri WHERE id = :musicId")
    suspend fun updateAlbumArt(musicId: String, newUri : String)

    @Query("SELECT * FROM music_table ORDER BY playCount DESC LIMIT :limit")
    suspend fun getTopMusic(limit : Int) : List<MusicEntity> // 리캡 용으로 재생횟수를 내림차순(DESC)으로 정렬하여 원하는 개수만큼 반환.
}