package com.example.musiclog

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.musiclog.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var musicRepository: MusicRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 👈 1. Hilt가 준비를 마치는 시점!


        lifecycleScope.launch {
            Log.d("MusicLogTest", "=== 유튜브 검색 테스트 시작 ===")
            val result = musicRepository.searchMusic("아이유")

            Log.d("MusicLogTest", "검색 결과 총 ${result.size}개 발견!")

            result.forEachIndexed { index, music ->
                Log.d("MusicLogTest", "[${index + 1}] 제목: ${music.title} / 가수: ${music.artist}")
                Log.d("MusicLogTest", "썸네일 주소: ${music.albumArtUrl}")
            }
            Log.d("MusicLogTest", "=== 테스트 종료 ===")
        }

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("MusicLog API Test")
                }
            }

        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

