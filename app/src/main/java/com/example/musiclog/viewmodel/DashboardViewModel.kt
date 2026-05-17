package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository : MusicRepository
) : ViewModel() {

    val uiState : StateFlow<DashboardUiState> = repository.getAllMusic()     // 로컬 DB의 음악 데이터 스트림을 ui state로 안전하게 변환하여 노출
        .map { musicList -> DashboardUiState.Success(musicList) }
        .catch { exception -> DashboardUiState.Error(exception.message ?: "알 수 없는 오류가 발생했습니다.") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // UI가 멈추면 5초 뒤 관찰 중단 (메모리 절약)
            initialValue = DashboardUiState.Loading
        )

    fun incrementMusicPlayCount(musicId : String) {     // 곡 재생 횟수 증가 로직 호출
        viewModelScope.launch {
            repository.incrementPlayCount(musicId)
        }
    }
}


sealed interface DashboardUiState { // 대시보드 화면의 세 가지 상태(로딩, 성공, 에러)를 안전하게 처리하기 위한 state 캡슐화 인터페이스
    object Loading : DashboardUiState
    data class Success(val songs: List<Music>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}