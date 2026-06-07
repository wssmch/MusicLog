package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musiclog.data.local.MusicDao
import com.example.musiclog.data.mapper.toEntity
import com.example.musiclog.data.remote.FirebaseClient
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val musicDao: MusicDao,
    private val firebaseClient: FirebaseClient
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Music>>(emptyList())
    val searchResults: StateFlow<List<Music>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val results = repository.searchMusic(query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    fun insertMusicToLog(music: Music) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"

            // 1. 로컬 DB 덮어쓰기 방지 (기존 횟수 불러와서 +1 더하기)
            val existingEntity = musicDao.getMusicById(music.id)
            val newPlayCount = if (existingEntity != null) existingEntity.playCount + 1 else 1

            val updatedMusic = music.toEntity().copy(
                playCount = newPlayCount,
                lastPlayedTimeStamp = System.currentTimeMillis()
            )
            musicDao.insertMusic(updatedMusic) // 로컬 저장 완료

            // 💡 2. 오프라인 누수 복구: 로컬 DB에 안전하게 기록된 해당 아티스트의 '절대값' 총합 연산
            val allMusic = musicDao.getAllMusic().first()
            val absoluteArtistCount = allMusic.filter { it.artist == music.artist }.sumOf { it.playCount }

            // 💡 3. Firebase 실시간 랭킹 강제 교정 동기화
            firebaseClient.syncArtistPlayCount(music.artist, uid, absoluteArtistCount)

            // 4. 클라우드 동기화 (히스토리)
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("history").document(music.id)
                .set(updatedMusic)
        }
    }

    fun updateMusicAlbumArt(musicId: String, newUri: String) {
        viewModelScope.launch { repository.updateAlbumArt(musicId, newUri) }
    }
}