package com.example.musiclog.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musiclog.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") } // 사용자가 입력하는 검색어(state)

    val searchResults by viewModel.searchResults.collectAsState() // 뷰모델의 상태 관찰
    val isLoading by viewModel.isLoading.collectAsState()

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

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
                            .clickable {
                                // search뷰모델을 통해 로컬 DB에 음악 로그 저장
                                viewModel.insertMusicToLog(music)

                                // 유튜브 고유 비디오 ID를 링크로 변환하여 기기 내 유튜브 앱에서 재생하는 트리거
                                uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
                            }
                    ) {
                        // 💡 Row를 사용해 가로로 배치 (왼쪽: 이미지, 오른쪽: 텍스트)
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 🖼️ Coil을 이용한 비동기 이미지 로딩
                            AsyncImage(
                                model = music.albumArtUrl, // 모델 클래스의 썸네일 주소 변수명 (맞게 수정해 주세요)
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