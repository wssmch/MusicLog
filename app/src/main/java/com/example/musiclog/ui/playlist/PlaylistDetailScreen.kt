package com.example.musiclog.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add // 💡 신규 임포트
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musiclog.domain.model.Music
import com.example.musiclog.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.getPlaylistDetail(playlistId).collectAsState(initial = null)
    val uriHandler = LocalUriHandler.current

    // 💡 해결: 상세 화면 전용 내부 트랙 추가 다이얼로그 제어 상태 변수 레이어 가동
    var showAddMusicDialog by remember { mutableStateOf(false) }
    val allMusicLog by viewModel.getAllMusicLog().collectAsState(initial = emptyList())

    if (showAddMusicDialog) {
        AlertDialog(
            onDismissRequest = { showAddMusicDialog = false },
            title = { Text("재생목록에 곡 추가") },
            text = {
                if (allMusicLog.isEmpty()) {
                    Text("로컬에 저장된 음악 로그 데이터가 없습니다. 먼저 홈이나 검색에서 음악을 재생해 보세요.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allMusicLog) { music ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addMusicToPlaylist(playlistId, music)
                                        showAddMusicDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = music.customAlbumArtUri ?: music.albumArtUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = music.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = music.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMusicDialog = false }) { Text("닫기") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailState?.title ?: "재생목록 상세") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                // 💡 해결: 수동으로 트랙을 관계형 테이블 구조에 매핑 인서트할 수 있는 TopAppBar 액션 버튼 개설
                actions = {
                    IconButton(onClick = { showAddMusicDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "곡 추가")
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
            detailState?.let { detail ->
                if (detail.songs.isEmpty()) {
                    Text(
                        text = "이 재생목록에 담긴 노래가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = detail.coverUri ?: "https://picsum.photos/seed/default/400/400",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "총 ${detail.songs.size}곡 수록",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        items(detail.songs) { music ->
                            DetailMusicItemRow(
                                music = music,
                                onClick = {
                                    uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
                                },
                                onRemoveClick = {
                                    viewModel.removeMusicFromPlaylist(detail.id, music.id)
                                }
                            )
                        }
                    }
                }
            } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun DetailMusicItemRow(
    music: Music,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = music.customAlbumArtUri ?: music.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(55.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "곡 옵션")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("재생목록에서 제거", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onRemoveClick()
                        }
                    )
                }
            }
        }
    }
}