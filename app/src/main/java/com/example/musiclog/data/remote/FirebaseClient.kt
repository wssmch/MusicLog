package com.example.musiclog.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor( // FirebaseDatabase 인스턴스를 주입받아 특정 아티스트의 전역 재생 카운트를 원자적으로 증가시키는
                                          // 함수를 구현. ServerValue.increment(1)를 사용하여 실시간 팬덤 랭킹 데이터에 활용
    private val database: FirebaseDatabase
) {
    fun incrementArtistPlayCount(artistName: String) {
        if (artistName.isBlank()) return
        database.getReference("artists")
            .child(artistName)
            .child("playCount")
            .setValue(ServerValue.increment(1))
    }
}