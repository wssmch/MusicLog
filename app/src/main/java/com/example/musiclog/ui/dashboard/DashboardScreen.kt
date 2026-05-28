package com.example.musiclog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musiclog.ui.components.MusicItem
import com.example.musiclog.viewmodel.DashboardUiState
import com.example.musiclog.viewmodel.DashboardViewModel

/**
 * 앱의 메인 진입점이 되는 대시보드 화면입니다.
 * 저장된 음악 로그 리스트와 최다 재생 리캡 통계를 보여줍니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToRanking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MusicLog", fontWeight = FontWeight.Bold) },
                actions = {
                    Button(onClick = onNavigateToRanking) {
                        Text("팬덤 랭킹")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNavigateToSearch) {
                        Text("음악 검색")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DashboardUiState.Success -> {
                    val songs = state.songs

                    if (songs.isEmpty()) {
                        Text(
                            text = "아직 기록된 음악 로그가 없습니다.\n우측 상단 검색 버튼을 눌러 음악을 추가해보세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 🔥 1. 유저 리캡 (최다 재생 곡 Top 3 시각화 섹션)
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🔥 나의 리캡 (최다 재생)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                val topSongs = songs.sortedByDescending { it.playCount }.take(3)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    topSongs.forEachIndexed { index, music ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${index + 1}위. ${music.title}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${music.playCount}회 재생",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 📝 2. 전체 음악 로그 리스트 섹션
                            item {
                                Text(
                                    text = "📁 내 음악 로그 전체 목록",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(songs) { music ->
                                MusicItem(
                                    music = music,
                                    onClick = {
                                        // 로그 카드를 누르면 재생 횟수가 1 올라가고 로컬/원격 DB에 동시 카운팅됨
                                        viewModel.incrementMusicPlayCount(music.id)
                                        // 실제 유튜브 인텐트를 호출하여 기기에서 끊김 없이 재생 처리
                                        uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}