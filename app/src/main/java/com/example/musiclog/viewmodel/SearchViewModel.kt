package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    // 검색 결과를 담는 상태 흐름, UI가 이 변수를 구독
    private val _searchResults = MutableStateFlow<List<Music>>(emptyList())
    val searchResults: StateFlow<List<Music>> = _searchResults.asStateFlow()

    // 로딩 상태를 보여주는 플래그
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI에서 검색 버튼을 누르면 호출할 함수
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
        viewModelScope.launch {
            repository.insertMusic(music)
        }
    }

    // 💡 검색 화면 내 옵션 시트(앨범 아트 변경)를 안전하게 처리하기 위한 신규 확장 함수
    fun updateMusicAlbumArt(musicId: String, newUri: String) {
        viewModelScope.launch {
            repository.updateAlbumArt(musicId, newUri)
        }
    }
}