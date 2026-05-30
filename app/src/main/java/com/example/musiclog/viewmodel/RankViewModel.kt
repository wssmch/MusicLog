package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface RankUiState {
    object Loading : RankUiState
    data class Success(
        val rankings: List<ArtistRanking>,
        val myUuid: String
    ) : RankUiState
    data class Error(val message: String) : RankUiState
}

data class ArtistRanking(
    val artistName: String,
    val totalPlayCount: Long,
    val listeners: Map<String, Long> = emptyMap()
)

@HiltViewModel
open class RankViewModel @Inject constructor() : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val rankingsRef = database.getReference("rankings")

    private val _uiState = MutableStateFlow<RankUiState>(RankUiState.Loading)
    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    // 디바이스 고유 식별자 임시 고정 (추후 SharedPreferences UUID 동기화 연동)
    val myUuid: String = "device_user_uuid_placeholder"

    init {
        fetchRankings()
    }

    private fun fetchRankings() {
        rankingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rankingList = mutableListOf<ArtistRanking>()
                for (artistSnapshot in snapshot.children) {
                    val artistName = artistSnapshot.key ?: continue
                    val totalPlayCount = artistSnapshot.child("totalPlayCount").getValue(Long::class.java) ?: 0L

                    val listenersMap = mutableMapOf<String, Long>()
                    val listenersSnapshot = artistSnapshot.child("listeners")
                    for (listener in listenersSnapshot.children) {
                        val uid = listener.key ?: continue
                        val count = listener.getValue(Long::class.java) ?: 0L
                        listenersMap[uid] = count
                    }

                    rankingList.add(
                        ArtistRanking(
                            artistName = artistName,
                            totalPlayCount = totalPlayCount,
                            listeners = listenersMap
                        )
                    )
                }
                // 공식 재생 횟수(totalPlayCount) 기준 글로벌 내림차순 정렬
                val sortedList = rankingList.sortedByDescending { it.totalPlayCount }
                _uiState.value = RankUiState.Success(sortedList, myUuid)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = RankUiState.Error(error.message)
            }
        })
    }
}