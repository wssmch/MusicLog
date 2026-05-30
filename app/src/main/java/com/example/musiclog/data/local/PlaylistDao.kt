package com.example.musiclog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistMusicCrossRef(crossRef: PlaylistMusicCrossRef)

    @Transaction
    @Query("SELECT * FROM playlist_table ORDER BY createdAt DESC")
    fun getPlaylistsWithMusic(): Flow<List<PlaylistWithMusic>>

    @Query("UPDATE playlist_table SET coverUri = :newUri WHERE playlistId = :id")
    suspend fun updatePlaylistCover(id: String, newUri: String)

    @Query("DELETE FROM playlist_table WHERE playlistId = :id")
    suspend fun deletePlaylist(id: String)
}