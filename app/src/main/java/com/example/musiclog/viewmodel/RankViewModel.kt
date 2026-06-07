package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface RankUiState {
    object Loading : RankUiState
    data class Success(
        val rankings: List<ArtistRanking>,
        val myUuid: String
    ) : RankUiState
    data class Error(val message: String) : RankUiState
}

data class ListenerProfile(
    val uid: String,
    val nickname: String,
    val playCount: Long
)

data class ArtistRanking(
    val artistName: String,
    val totalPlayCount: Long,
    val listeners: List<ListenerProfile> = emptyList()
)

@HiltViewModel
open class RankViewModel @Inject constructor() : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val rankingsRef = database.getReference("rankings")
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<RankUiState>(RankUiState.Loading)
    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    val myUuid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"

    private val nicknameCache = mutableMapOf<String, String>()
    private var rankingListener: ValueEventListener? = null

    init {
        refreshRankings()
    }

    fun refreshRankings() {
        _uiState.value = RankUiState.Loading
        nicknameCache.clear()

        rankingListener?.let { rankingsRef.removeEventListener(it) }

        rankingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val rankingList = mutableListOf<ArtistRanking>()

                    for (artistSnapshot in snapshot.children) {
                        val artistName = artistSnapshot.key ?: continue
                        val totalPlayCount = artistSnapshot.child("totalPlayCount").getValue(Long::class.java) ?: 0L
                        val listenersSnapshot = artistSnapshot.child("listeners")

                        if (!listenersSnapshot.hasChild(myUuid)) continue

                        val listenersList = mutableListOf<ListenerProfile>()

                        for (listener in listenersSnapshot.children) {
                            val uid = listener.key ?: continue

                            // 💡 수정됨: 파싱 크래시를 방지하기 위한 안전한 데이터 추출 로직
                            val count = listener.child("playCount").getValue(Long::class.java)
                                ?: listener.getValue(Long::class.java)
                                ?: 0L

                            val nickname = if (nicknameCache.containsKey(uid)) {
                                nicknameCache[uid].orEmpty()
                            } else {
                                try {
                                    val document = firestore.collection("users").document(uid).get().await()
                                    val name = document.getString("nickname") ?: "일반 리스너"
                                    nicknameCache[uid] = name
                                    name
                                } catch (e: Exception) {
                                    "일반 리스너"
                                }
                            }

                            listenersList.add(
                                ListenerProfile(uid = uid, nickname = nickname, playCount = count)
                            )
                        }

                        // 해당 아티스트 내에서 리스너들의 랭킹(내림차순) 정렬 보장
                        listenersList.sortByDescending { it.playCount }

                        rankingList.add(
                            ArtistRanking(
                                artistName = artistName,
                                totalPlayCount = totalPlayCount,
                                listeners = listenersList
                            )
                        )
                    }

                    val sortedList = rankingList.sortedByDescending { ranking ->
                        ranking.listeners.find { it.uid == myUuid }?.playCount ?: 0L
                    }
                    _uiState.value = RankUiState.Success(sortedList, myUuid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = RankUiState.Error(error.message)
            }
        }

        rankingsRef.addValueEventListener(rankingListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        rankingListener?.let { rankingsRef.removeEventListener(it) }
    }
}