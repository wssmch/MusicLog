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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreen(
    viewModel: RankViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("글로벌 팬덤 랭킹", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
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
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is RankUiState.Success -> {
                    val rankings = state.rankings
                    val myUuid = state.myUuid

                    if (rankings.isEmpty()) {
                        Text(
                            text = "집계된 글로벌 랭킹 데이터가 없습니다.",
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
                            item { Spacer(modifier = Modifier.height(4.dp)) }

                            itemsIndexed(rankings) { index, ranking ->
                                RankArtistItem(
                                    rank = index + 1,
                                    ranking = ranking,
                                    myUuid = myUuid
                                )
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
fun RankArtistItem(
    rank: Int,
    ranking: ArtistRanking,
    myUuid: String
) {
    val listenersMap = ranking.listeners
    val totalListeners = listenersMap.size

    // 클라이언트 사이드 백분위 정밀 연산식 적용
    // 공식: 백분위 = (1.0 - (나보다 청취 횟수가 적은 유저 수 / 전체 청취자 수)) * 100
    val percentileText = if (totalListeners > 0 && listenersMap.containsKey(myUuid)) {
        val myPlayCount = listenersMap[myUuid] ?: 0L
        val fewerCount = listenersMap.values.count { it < myPlayCount }
        val percentile = (1.0 - (fewerCount.toDouble() / totalListeners.toDouble())) * 100.0
        String.format("상위 %.1f%%", percentile)
    } else {
        "데이터 없음"
    }

    // 유튜브 뮤직 스타일 고유 기여도 뱃지 판정 규칙
    val badgeName = if (percentileText != "데이터 없음") {
        val myPlayCount = listenersMap[myUuid] ?: 0L
        val fewerCount = listenersMap.values.count { it < myPlayCount }
        val percentile = (1.0 - (fewerCount.toDouble() / totalListeners.toDouble())) * 100.0
        when {
            percentile <= 1.0 -> " 명예의 전당 뱃지"
            percentile <= 5.0 -> " 골드 스트리머 뱃지"
            percentile <= 10.0 -> " 실버 리스너 뱃지"
            else -> " 일반 청취 리스너"
        }
    } else {
        "미집계 리스너"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${rank}위",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(45.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ranking.artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(badgeName) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = percentileText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "🎧 ${ranking.totalPlayCount}회",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}