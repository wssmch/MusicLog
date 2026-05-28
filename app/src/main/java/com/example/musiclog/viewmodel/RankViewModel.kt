package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ArtistRanking(
    val artistName: String = "",
    val playCount: Long = 0
)

sealed interface RankUiState {
    object Loading : RankUiState
    data class Success(val rankings: List<ArtistRanking>) : RankUiState
    data class Error(val message: String) : RankUiState
}

@HiltViewModel
class RankViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    val uiState: StateFlow<RankUiState> = callbackFlow {
        val ref = firebaseDatabase.getReference("artists")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rankingList = mutableListOf<ArtistRanking>()
                for (child in snapshot.children) {
                    val name = child.key ?: continue
                    val count = child.child("playCount").getValue(Long::class.java) ?: 0L
                    rankingList.add(ArtistRanking(artistName = name, playCount = count))
                }
                // 재생 횟수 기준 내림차순 정렬하여 순위 배정
                val sortedList = rankingList.sortedByDescending { it.playCount }
                trySend(RankUiState.Success(sortedList))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(RankUiState.Error(error.message))
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RankUiState.Loading
    )
}