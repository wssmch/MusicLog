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
import kotlinx.coroutines.withContext

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MusicRepository, private val database: MusicDatabase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        repository.getAllMusic()     // 로컬 DB의 음악 데이터 스트림을 ui state로 안전하게 변환하여 노출
            .map { musicList -> DashboardUiState.Success(musicList) }.catch { exception ->
                DashboardUiState.Error(
                    exception.message ?: "알 수 없는 오류가 발생했습니다."
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // UI가 멈추면 5초 뒤 관찰 중단 (메모리 절약)
                initialValue = DashboardUiState.Loading
            )

    fun incrementMusicPlayCount(musicId: String) {     // 곡 재생 횟수 증가 로직 호출
        viewModelScope.launch {
            repository.incrementPlayCount(musicId)
        }
    }

    fun updateMusicAlbumArt(musicId: String, newUri: String) {
        viewModelScope.launch {
            repository.updateAlbumArt(musicId, newUri)
        }
    }

//    fun logoutAndClearData(onComplete: () -> Unit) {
//        // NonCancellable을 적용하여 뷰모델이 파괴되어도 백업 트랜잭션이 끝까지 보장되도록 수정
//        viewModelScope.launch {
//            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
//                try {
//                    repository.backupLocalDataToCloud()
//                } catch (e: Exception) {
//                    android.util.Log.e("MusicLog", "Cloud Backup Failed: ${e.message}")
//                } finally {
//                    database.clearAllTables()
//                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
//                }
//            }
//            onComplete()
//        }
//    }

    fun logoutAndClearData(onComplete: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                try {
                    android.util.Log.d("test", "1. 백업 트랜잭션 시작")
                    repository.backupLocalDataToCloud()
                    android.util.Log.d("test", "2. 백업 트랜잭션 완료")
                } catch (e: Exception) {
                    android.util.Log.e("test", "백업 실패 에러: ${e.message}", e)
                } finally {
                    android.util.Log.d("test", "3. 로컬 Room DB 테이블 파기 시작")
                    database.clearAllTables()
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    android.util.Log.d("test", "4. 로그아웃 세션 종료 완료")
                }
            }
            onComplete()
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncAndLoadUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                android.util.Log.d("test", "5. 클라우드 데이터 다운로드 시작")
                repository.restoreCloudDataToLocal()
                android.util.Log.d("test", "6. 클라우드 데이터 로컬 복원 완료")
            } catch (e: Exception) {
                android.util.Log.e("test", "복원 실패 에러: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }



    sealed interface DashboardUiState { // 대시보드 화면의 세 가지 상태(로딩, 성공, 에러)를 안전하게 처리하기 위한 state 캡슐화 인터페이스
        object Loading : DashboardUiState
        data class Success(val songs: List<Music>) : DashboardUiState
        data class Error(val message: String) : DashboardUiState
    }
}