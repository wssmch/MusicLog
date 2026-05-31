package com.example.musiclog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musiclog.domain.model.Music
import com.example.musiclog.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PlayCountViewModel @Inject constructor(
    repository: MusicRepository
) : ViewModel() {

    // 로컬 검색어 상태 관리
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 💡 해결: 전체 음악 스트림과 검색어 스트림을 결합(combine)하여 로컬 필터링 및 내림차순 정렬 수행
    val filteredSortedMusic: StateFlow<List<Music>> = combine(
        repository.getAllMusic(),
        _searchQuery
    ) { musicList, query ->
        // 1. 재생 횟수 기준 내림차순 정렬
        val sortedList = musicList.sortedByDescending { it.playCount }

        // 2. 검색어 필터링 (대소문자 구분 없이 제목 또는 아티스트에 포함되는지 확인)
        if (query.isBlank()) {
            sortedList
        } else {
            sortedList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}