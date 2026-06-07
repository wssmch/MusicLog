package com.example.musiclog.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.musiclog.domain.model.Music
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PlayerState(
    val currentMusic: Music? = null,
    val queue: List<Music> = emptyList(),
    val currentIndex: Int = -1
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        restoreLastPlayedMusic()
    }

    fun playMusic(music: Music, newQueue: List<Music>? = null) {
        _playerState.update { currentState ->
            val updatedQueue = newQueue ?: currentState.queue
            val newIndex = updatedQueue.indexOf(music).takeIf { it >= 0 } ?: 0
            saveLastPlayedMusic(music)
            currentState.copy(currentMusic = music, queue = updatedQueue, currentIndex = newIndex)
        }
    }

    fun skipToNext(): Music? {
        var nextMusic: Music? = null
        _playerState.update { currentState ->
            if (currentState.queue.isEmpty() || currentState.currentIndex == -1) return@update currentState
            val nextIndex = if (currentState.currentIndex + 1 < currentState.queue.size) currentState.currentIndex + 1 else 0
            nextMusic = currentState.queue[nextIndex]
            saveLastPlayedMusic(nextMusic!!)
            currentState.copy(currentMusic = nextMusic, currentIndex = nextIndex)
        }
        return nextMusic
    }

    fun skipToPrevious(): Music? {
        var prevMusic: Music? = null
        _playerState.update { currentState ->
            if (currentState.queue.isEmpty() || currentState.currentIndex == -1) return@update currentState
            val prevIndex = if (currentState.currentIndex - 1 >= 0) currentState.currentIndex - 1 else currentState.queue.size - 1
            prevMusic = currentState.queue[prevIndex]
            saveLastPlayedMusic(prevMusic!!)
            currentState.copy(currentMusic = prevMusic, currentIndex = prevIndex)
        }
        return prevMusic
    }

    private fun saveLastPlayedMusic(music: Music) {
        prefs.edit().apply {
            putString("last_music_id", music.id)
            putString("last_music_title", music.title)
            putString("last_music_artist", music.artist)
            putString("last_music_art", music.albumArtUrl)
            apply()
        }
    }

    private fun restoreLastPlayedMusic() {
        val id = prefs.getString("last_music_id", null) ?: return
        val title = prefs.getString("last_music_title", "") ?: ""
        val artist = prefs.getString("last_music_artist", "") ?: ""
        val artUrl = prefs.getString("last_music_art", "") ?: ""
        val restoredMusic = Music(id = id, title = title, artist = artist, albumArtUrl = artUrl, customAlbumArtUri = null, playCount = 0)
        _playerState.update { it.copy(currentMusic = restoredMusic) }
    }
}