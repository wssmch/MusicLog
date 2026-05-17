package com.example.musiclog.domain.repository

import android.net.Uri
import com.example.musiclog.domain.model.Music
import kotlinx.coroutines.flow.Flow

interface MusicRepository{
    fun getAllMusic() : Flow<List<Music>> //Flow 데이터 스트림을 써서 DB의 데이터가 변경될 때 함수의 재호출 없이 ui에 바로 전달
                                         // domain 레이어에서는 MusicEntity로 바꾸면 도메인 레이어가 데이터 레이어(room변수)에세
                                         // 의존하게 되어 계층 구조의 분리가 깨짐
    suspend fun searchMusic(query : String) : List<Music>

    suspend fun incrementPlayCount(musicId : String)
    suspend fun updateAlbumArt(musicId : String, newUri : String)

    suspend fun getTopMusic(limit : Int) : List<Music> //나중에 구현할 리캡 탭을 누르면 재생횟수가 높은 곡들을 출력
}