package com.example.musiclog.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musiclog.viewmodel.DashboardUiState
import com.example.musiclog.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToPlayCount: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // 이미지 수정을 원하는 곡의 ID를 임시 보관하는 상태 상태 변수
    var targetMusicIdForArt by remember { mutableStateOf<String?>(null) }

    // 안드로이드 내장 포토 갤러리 액티비티 호출 컨트랙트 정의
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            targetMusicIdForArt?.let { musicId ->
                try {
                    // 안드로이드 OS로부터 해당 로컬 파일 URI에 대한 영구적 읽기 권한을 받아옴. 즉 앱 재시작 후 엑스박스 방지
                    context.contentResolver.takePersistableUriPermission( //
                        selectedImageUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dashboardViewModel.updateMusicAlbumArt(musicId, selectedImageUri.toString())
            }
        }
    }

    Scaffold(
        modifier = modifier.navigationBarsPadding(), // 만약 하단바가 스와이프로 올라와도 버튼이 안 가려지게 여백 확보
        topBar = {
            TopAppBar(
                title = { Text("MusicLog", fontWeight = FontWeight.Bold) },
                actions = {
                    Button(onClick = onNavigateToSearch) {
                        Text("음악 검색")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNavigateToRanking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("랭킹")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToPlayCount,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("재생횟수")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToPlaylist,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("재생목록")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = dashboardState) {
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. 빠른 선곡 섹션 (3*3 배열 격자 레이아웃)
                        item {
                            Text(
                                text = "빠른 선곡",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // 곡 리스트가 비어있어도 더미로 채우기 위해 isEmpty 분기를 제거하고 통합
                            val topSongs = songs.sortedByDescending { it.playCount }.take(9).toMutableList()

                            // 9개 미만일 경우 임의의 썸네일(더미 데이터)로 칸을 채워 3x3 바둑판 고정
                            var dummyIdCounter = 0
                            while (topSongs.size < 9) {
                                topSongs.add(
                                    com.example.musiclog.domain.model.Music(
                                        id = "dummy_${dummyIdCounter++}",
                                        title = "추천 곡",
                                        artist = "추천 아티스트",
                                        albumArtUrl = "https://picsum.photos/seed/${100 + dummyIdCounter}/300/300", // 임의의 샘플 이미지
                                        customAlbumArtUri = null,
                                        playCount = 0,
                                        lastPlayedTimeStamp = 0L
                                    )
                                )
                            }

                            // 💡 격자 구조 정립: 3개씩 묶어 가로 줄(Row) 단위로 분할 수용
                            val chunkedSongs = topSongs.chunked(3)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp), // 💡 여백 확대
                                verticalArrangement = Arrangement.spacedBy(16.dp) // 💡 줄 간격 확대
                            ) {
                                chunkedSongs.forEach { rowSongs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp) // 💡 항목 간격 확대
                                    ) {
                                        rowSongs.forEach { music ->
                                            // 격자 내의 개별 컴팩트 아이템 (이미지가 위, 텍스트가 아래로 오도록 바둑판 배열)
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f) // 가로 공간을 균등하게 삼분할
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (!music.id.startsWith("dummy")) {
                                                                dashboardViewModel.incrementMusicPlayCount(music.id)
                                                                uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!music.id.startsWith("dummy")) {
                                                                targetMusicIdForArt = music.id
                                                                galleryLauncher.launch("image/*")
                                                            }
                                                        }
                                                    )
                                                    .padding(6.dp), // 💡 내부 패딩 확대
                                                horizontalAlignment = Alignment.CenterHorizontally // 아이템 가운데 정렬
                                            ) {
                                                // 💡 사용자 편집 이미지 유지 로직 보존
                                                AsyncImage(
                                                    model = music.customAlbumArtUri ?: music.albumArtUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .aspectRatio(1f) // 💡 정사각형 썸네일로 고정
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp)), // 💡 곡률 확대
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(8.dp)) // 💡 이미지-텍스트 간격 조정

                                                Text(
                                                    text = music.title,
                                                    style = MaterialTheme.typography.titleSmall, // 💡 곡 제목 글꼴 확대 (bodyMedium -> titleSmall)
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = music.artist,
                                                    style = MaterialTheme.typography.bodyMedium, // 💡 아티스트 글꼴 확대 (bodySmall -> bodyMedium)
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        // 3개 미만으로 남은 행의 비율 깨짐 방지용 공백 채우기 (9개를 채우므로 호출될 일은 거의 없음)
                                        if (rowSongs.size < 3) {
                                            repeat(3 - rowSongs.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}