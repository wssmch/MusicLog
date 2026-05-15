package com.example.musiclog.di

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module // YouTube API v3 및 Firebase Realtime Database 인스턴스를 제공하는 Hilt 모듈입니다.
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val YOUTUBE_API_KEY = "AIzaSyBhzKfwv-q8rb0dGrL1lLenQLridONeStA"

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase { // 팬덤 랭킹을 위한 firebase db객체
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideYouTubeService(): YouTube { // youtube검색을 위한 서비스 객체
        return YouTube.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            null
        ).setApplicationName("MusicLog")
            .setYouTubeRequestInitializer(YouTubeRequestInitializer(YOUTUBE_API_KEY))
            .build()
    }
}