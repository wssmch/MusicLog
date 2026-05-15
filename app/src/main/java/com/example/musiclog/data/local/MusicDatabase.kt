package com.example.musiclog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MusicEntity::class], version = 1, exportSchema = false) // exportSchema는 별도의 스키마 히스토리를 설정하는 파라미터
abstract class MusicDatabase : RoomDatabase(){
    abstract fun musicDao() : MusicDao // Room이 컴파일을 수행할 때 실제 구현체를 생성 후 반환함
}