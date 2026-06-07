package com.example.musiclog.ui.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musiclog.viewmodel.ArtistRanking
import com.example.musiclog.viewmodel.RankUiState
import com.example.musiclog.viewmodel.RankViewModel
import androidx.compose.material.icons.filled.Refresh
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreen(
    viewModel: RankViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // LaunchedEffect 스코프 배치를 통해 화면 최초 컴포지션 시점에 비동기 동기화 자동 트리거
    LaunchedEffect(Unit) {
        viewModel.refreshRankings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("글로벌 랭킹", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                // 💡 상단 우측에 새로고침 액션 버튼 신규 배치
                actions = {
                    IconButton(onClick = { viewModel.refreshRankings() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
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
                is RankUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RankUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is RankUiState.Success -> {
                    val rankings = state.rankings
                    val myUuid = state.myUuid

                    if (rankings.isEmpty()) {
                        Text(text = "집계된 글로벌 랭킹 데이터가 없습니다.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                            itemsIndexed(rankings) { index, ranking ->
                                RankArtistItem(rank = index + 1, ranking = ranking, myUuid = myUuid)
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankArtistItem(rank: Int, ranking: ArtistRanking, myUuid: String) {
    val listenersList = ranking.listeners
    val totalListeners = listenersList.size
    val myProfile = listenersList.find { it.uid == myUuid }

    // 💡 백분율 수학 연산 오류 수정
    val percentileText: String
    val badgeName: String

    if (totalListeners > 0 && myProfile != null) {
        val myPlayCount = myProfile.playCount

        // 나보다 재생 횟수가 많은(뛰어난) 사람의 수 카운팅
        val betterCount = listenersList.count { it.playCount > myPlayCount }
        val myRank = betterCount + 1 // 1등이면 betterCount가 0이므로 내 등수는 1

        // 💡 수정됨: 모수가 적을 때 발생하는 백분율 왜곡 방지 및 1등 절대 보장 로직
        val percentile = when {
            totalListeners == 1 -> 1.0 // 나 혼자면 1% (명예의 전당)
            myRank == 1 -> 1.0 // 전체 인원이 몇 명이든 내가 1등이면 무조건 1.0% 고정
            else -> {
                // 1등이 아닌 경우: (나를 이긴 사람 수 / 전체 인원) * 100
                // 예: 10명 중 2등이면 (1 / 10) * 100 = 10.0% (실버 리스너)
                (betterCount.toDouble() / totalListeners.toDouble()) * 100.0
            }
        }

        percentileText = String.format(java.util.Locale.getDefault(), "상위 %.1f%%", percentile)
        badgeName = when {
            percentile <= 1.0 -> "🏆 명예의 전당"
            percentile <= 5.0 -> "🥇 골드 리스너"
            percentile <= 10.0 -> "🥈 실버 리스너"
            else -> "🎧 일반 리스너"
        }
    } else {
        percentileText = "데이터 없음"
        badgeName = "미집계 리스너"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 내 관심도 순위 (내가 좋아하는 아티스트 1위, 2위...)
            Text(text = "${rank}위", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(45.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 아티스트 명
                Text(text = ranking.artistName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))

                // 최고 점수를 구한 뒤, 그 점수를 가진 모든 동점자를 리스트로 묶어 공동 출력
                val maxPlayCount = listenersList.maxOfOrNull { it.playCount } ?: 0L
                val topListeners = listenersList.filter { it.playCount == maxPlayCount && maxPlayCount > 0 }

                if (topListeners.isNotEmpty()) {
                    val topNames = topListeners.joinToString(", ") { it.nickname }
                    Text(text = "👑 탑 팬: $topNames (${maxPlayCount}회)",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(onClick = {}, label = { Text(badgeName) }, modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = percentileText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                }
            }
            // 우측: 이 아티스트에 대한 나의 재생 횟수
            Text(text = "🎧 ${myProfile?.playCount ?: 0}회", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}