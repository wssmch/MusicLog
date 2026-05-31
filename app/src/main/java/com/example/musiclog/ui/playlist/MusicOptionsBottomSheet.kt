package com.example.musiclog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musiclog.domain.model.Music

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicOptionsBottomSheet(
    music: Music,
    onDismissRequest: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onChangeAlbumArtClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "'${music.title}'",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddToPlaylistClick() }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text("🎵 재생목록에 추가", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeAlbumArtClick() }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text("🖼️ 대표 이미지 변경", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}