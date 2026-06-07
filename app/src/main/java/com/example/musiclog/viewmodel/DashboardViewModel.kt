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
import com.example.musiclog.data.local.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val database: MusicDatabase
) : ViewModel() {

    val currentUserEmail: String
        get() = FirebaseAuth.getInstance().currentUser?.email ?: "로그인 정보 없음"

    // 💡 복구: DashboardScreen에서 state.songs로 접근하므로 Success 데이터 스키마와 정렬 파이프라인 유지
    val uiState: StateFlow<DashboardUiState> =
        repository.getAllMusic()
            .map { musicList -> DashboardUiState.Success(musicList) }
            .catch { exception ->
                DashboardUiState.Error(exception.message ?: "알 수 없는 오류가 발생했습니다.")
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DashboardUiState.Loading
            )

    // 💡 복구: 누락되었던 앨범 아트 커스텀 경로 가동 함수 복원
    fun updateMusicAlbumArt(musicId: String, newUri: String) {
        viewModelScope.launch {
            repository.updateAlbumArt(musicId, newUri)
        }
    }

    // 💡 유지: 불필요한 일괄 백업 처리가 차단된 청소 트랜잭션 함수
    fun logoutAndClearData(onComplete: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    android.util.Log.d("test", "1. 로컬 Room DB 테이블 파기 시작")
                    database.clearAllTables()
                    FirebaseAuth.getInstance().signOut()
                    android.util.Log.d("test", "2. 로그아웃 세션 종료 완료")
                } catch (e: Exception) {
                    android.util.Log.e("test", "로그아웃 실패 에러: ${e.message}", e)
                }
            }
            onComplete()
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var hasSynced = false

    // 💡 유지: 무분별한 덮어쓰기 무효화 락(Lock) 파이프라인
    fun syncAndLoadUserData() {
        if (hasSynced) return
        hasSynced = true

        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                // 💡 해결: 로컬 DB의 현재 상태를 1회 읽어옴
                val localData = repository.getAllMusic().first()

                if (localData.isEmpty()) {
                    // 새 디바이스 혹은 앱 삭제 후 재설치: 로컬이 비어있으므로 클라우드 히스토리를 쫙 땡겨옴
                    android.util.Log.d("test", "새 디바이스 감지: 클라우드에서 데이터를 복원합니다.")
                    repository.restoreCloudDataToLocal()
                } else {
                    // 기존 디바이스: 이미 로컬에 누적된 데이터가 있으므로 덮어쓰기(1회 리셋 버그) 원천 차단
                    android.util.Log.d("test", "기존 데이터 유지: 클라우드 덮어쓰기를 차단합니다.")
                }
            } catch (e: Exception) {
                android.util.Log.e("test", "동기화 에러: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    sealed interface DashboardUiState {
        object Loading : DashboardUiState
        // 💡 복구: musicList 변수명을 원래 규칙인 songs로 환원하여 스크린 결함 복구
        data class Success(val songs: List<Music>) : DashboardUiState
        data class Error(val message: String) : DashboardUiState
    }
}