package com.example.musiclog.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musiclog.viewmodel.DashboardViewModel
import com.example.musiclog.ui.playlist.PlaylistBottomSheet
import com.example.musiclog.ui.components.MusicOptionsBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.example.musiclog.utils.NetworkUtils
import androidx.compose.runtime.LaunchedEffect
import com.example.musiclog.domain.model.Music

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToPlayCount: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayMusic: (Music, List<Music>) -> Unit
) {

    LaunchedEffect(Unit) {
        android.util.Log.d("test", "DashboardScreen: LaunchedEffect 진입, 동기화 함수 호출함")
        dashboardViewModel.syncAndLoadUserData()
    }



    val playlistViewModel: com.example.musiclog.viewmodel.PlaylistViewModel = hiltViewModel()

    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var targetMusicIdForArt by remember { mutableStateOf<String?>(null) }

    // 💡 해결: 옵션 A 전용 흐름 관리를 위한 가시성 상태 변수 제어 블록 배치
    var selectedMusicForOptions by remember {
        mutableStateOf<com.example.musiclog.domain.model.Music?>(
            null
        )
    }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            targetMusicIdForArt?.let { musicId ->
                try {
                    context.contentResolver.takePersistableUriPermission(
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

    // 💡 1차 분기 지점: 공통 옵션 바텀 시트 노출 구역
    if (showOptionsSheet && selectedMusicForOptions != null) {
        MusicOptionsBottomSheet(
            music = selectedMusicForOptions!!,
            onDismissRequest = {
                showOptionsSheet = false
                selectedMusicForOptions = null
            },
            onAddToPlaylistClick = {
                showOptionsSheet = false
                showPlaylistSheet = true
            },
            onChangeAlbumArtClick = {
                showOptionsSheet = false
                targetMusicIdForArt = selectedMusicForOptions!!.id
                galleryLauncher.launch("image/*")
            }
        )
    }

    // 💡 2차 분기 지점: 수동 재생목록 리스트 팝업 바텀 시트 노출 구역
    if (showPlaylistSheet && selectedMusicForOptions != null) {
        PlaylistBottomSheet(
            music = selectedMusicForOptions!!,
            viewModel = playlistViewModel,
            onDismissRequest = {
                showPlaylistSheet = false
                selectedMusicForOptions = null
            }
        )
    }

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MusicLog", fontWeight = FontWeight.Bold)
                        // 💡 5번 요구사항: 현재 로그인된 이메일을 서브타이틀로 노출
                        Text(
                            text = dashboardViewModel.currentUserEmail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Button(onClick = {
                        // 💡 검색 화면 진입 전 네트워크 검사
                        if (NetworkUtils.isNetworkAvailable(context)) {
                            onNavigateToSearch()
                        } else {
                            Toast.makeText(context, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("음악 검색")
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // 신규 추가: 로그아웃 버튼 및 Activity 완전 재시작 로직
                    IconButton(
                        onClick = {
                            // 💡 핵심 버그 수정: 로그아웃 즉시 하단 바(미니플레이어) 임시 기억장치 파기
                            context.getSharedPreferences("music_player_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()

                            dashboardViewModel.logoutAndClearData {
                                val intent = android.content.Intent(context, com.example.musiclog.MainActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "로그아웃", tint = MaterialTheme.colorScheme.onSurface)
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // 💡 랭킹 화면 진입 전 네트워크 검사
                            if (NetworkUtils.isNetworkAvailable(context)) {
                                onNavigateToRanking()
                            } else {
                                Toast.makeText(context, "글로벌 랭킹은 온라인에서만 확인 가능합니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("랭킹")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToPlayCount, // 오프라인 허용 (로컬 DB)
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("재생횟수")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToPlaylist, // 오프라인 허용 (로컬 DB)
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
                is DashboardViewModel.DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is DashboardViewModel.DashboardUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DashboardViewModel.DashboardUiState.Success -> {
                    val songs = state.songs

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "빠른 선곡",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            val topSongs =
                                songs.sortedByDescending { it.playCount }.take(9).toMutableList()

                            var dummyIdCounter = 0
                            while (topSongs.size < 9) {
                                topSongs.add(
                                    com.example.musiclog.domain.model.Music(
                                        id = "dummy_${dummyIdCounter++}",
                                        title = "추천 곡",
                                        artist = "추천 아티스트",
                                        albumArtUrl = "https://picsum.photos/seed/${100 + dummyIdCounter}/300/300",
                                        customAlbumArtUri = null,
                                        playCount = 0,
                                        lastPlayedTimeStamp = 0L
                                    )
                                )
                            }

                            val chunkedSongs = topSongs.chunked(3)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                chunkedSongs.forEach { rowSongs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowSongs.forEach { music ->
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (!music.id.startsWith("dummy")) {
                                                                // 💡 삭제된 뷰모델 호출 대신, 상위(MainActivity)의 통합 콜백으로 제어권 위임
                                                                onPlayMusic(music, topSongs.filter { !it.id.startsWith("dummy") })
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!music.id.startsWith("dummy")) {
                                                                // 💡 해결: 직접 인텐트를 유도하는 대신 공통 옵션 변환 트리거 가동
                                                                selectedMusicForOptions = music
                                                                showOptionsSheet = true
                                                            }
                                                        }
                                                    )
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                AsyncImage(
                                                    model = music.customAlbumArtUri
                                                        ?: music.albumArtUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = music.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = music.artist,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
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