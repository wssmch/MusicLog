package com.example.musiclog.ui.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musiclog.viewmodel.SearchViewModel
import com.example.musiclog.ui.playlist.PlaylistBottomSheet
import com.example.musiclog.ui.components.MusicOptionsBottomSheet

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    // 💡 내부 스코프 뷰모델 조달
    val playlistViewModel: com.example.musiclog.viewmodel.PlaylistViewModel = hiltViewModel()

    var query by remember { mutableStateOf("") } // 사용자가 입력하는 검색어(state)

    val searchResults by viewModel.searchResults.collectAsState() // 뷰모델의 상태 관찰
    val isLoading by viewModel.isLoading.collectAsState()

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = LocalContext.current

    var targetMusicIdForArt by remember { mutableStateOf<String?>(null) }
    var selectedMusicForOptions by remember { mutableStateOf<com.example.musiclog.domain.model.Music?>(null) }
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
                viewModel.updateMusicAlbumArt(musicId, selectedImageUri.toString())
            }
        }
    }

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

                // 검색된 곡은 로컬 DB에 없을 수도 있으므로 선제적으로 저장 후 갤러리를 띄웁니다
                viewModel.insertMusicToLog(selectedMusicForOptions!!)
                galleryLauncher.launch("image/*")
            }
        )
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. 검색창과 버튼 레이아웃
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("검색어를 입력하세요 ") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.search(query) }) {
                Text("검색")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) { // 2. 결과 화면 분기 (로딩 중 vs 리스트 표시)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() // 검색 중임을 알려주는 인디케이터
            }
        } else {
            LazyColumn( // 결과 리스트 뿌리기
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { music ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable( // 💡 단순 clickable에서 롱클릭 지원으로 확장
                                onClick = {
                                    // search뷰모델을 통해 로컬 DB에 음악 로그 저장
                                    viewModel.insertMusicToLog(music)

                                    // 유튜브 고유 비디오 ID를 링크로 변환하여 기기 내 유튜브 앱에서 재생하는 트리거
                                    uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
                                },
                                onLongClick = {
                                    // 💡 검색 리스트에서도 롱클릭 시 옵션 시트 호출
                                    selectedMusicForOptions = music
                                    showOptionsSheet = true
                                }
                            )
                    ) {
                        // 💡 Row를 사용해 가로로 배치 (왼쪽: 이미지, 오른쪽: 텍스트)
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 🖼️ Coil을 이용한 비동기 이미지 로딩
                            AsyncImage(
                                model = music.customAlbumArtUri ?: music.albumArtUrl, // 커스텀 아트 우선 표기 처리
                                contentDescription = "썸네일 이미지",
                                modifier = Modifier
                                    .size(80.dp) // 썸네일 크기 지정
                                    .clip(RoundedCornerShape(8.dp)), // 모서리를 둥글게 깎아줌
                                contentScale = ContentScale.Crop // 이미지가 찌그러지지 않게 꽉 채움
                            )

                            // 이미지와 텍스트 사이의 여백
                            Spacer(modifier = Modifier.width(16.dp))

                            // 📝 기존 텍스트 컬럼
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = music.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2 // 제목이 너무 길면 2줄까지만 표시
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
        }
    }
}