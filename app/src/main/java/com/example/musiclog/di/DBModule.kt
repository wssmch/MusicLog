package com.example.musiclog.di

import android.content.Context
import androidx.room.Room
import com.example.musiclog.data.local.MusicDao
import com.example.musiclog.data.local.MusicDatabase
import com.example.musiclog.data.local.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DBModule {

    @Provides
    @Singleton
    fun provideMusicDataBase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context, MusicDatabase::class.java, "music_database"
        ).fallbackToDestructiveMigration() // 이 파이프라인 메서드가 추가되어야 마이그레이션 예외로 인한 앱 종료가 방지됨.
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicDao(database: MusicDatabase): MusicDao {
        return database.musicDao()
    }

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao {
        return database.playlistDao()
    }
}