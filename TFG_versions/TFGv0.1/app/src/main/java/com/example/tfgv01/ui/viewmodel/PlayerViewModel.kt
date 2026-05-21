// app/src/main/java/com/example/tfgv01/ui/viewmodel/PlayerViewModel.kt
package com.example.tfgv01.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tfgv01.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    private val _selectedInstrument = MutableStateFlow("guitar")
    val selectedInstrument: StateFlow<String> = _selectedInstrument.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTime = MutableStateFlow(0f)
    val currentTime: StateFlow<Float> = _currentTime.asStateFlow()

    // 🔊 Estado de silenciador del video (Mute)
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun loadSong(song: Song) {
        _song.value = song
        _isPlaying.value = false
        _currentTime.value = 0f
        _isMuted.value = false // Resetear audio al cambiar de canción
        _selectedInstrument.value = song.tabs.keys.firstOrNull() ?: "guitar"
    }

    fun selectInstrument(instrument: String) {
        if (_song.value?.hasTabFor(instrument) == true) {
            _selectedInstrument.value = instrument
        }
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    // 🔊 Alternar entre muteado y desmuteado
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun updateCurrentTime(seconds: Float) {
        _currentTime.value = seconds
    }

    fun seekTo(seconds: Float) {
        _currentTime.value = seconds
    }

    fun getTabAssetPath(): String? {
        val song = _song.value ?: return null
        val instrument = _selectedInstrument.value
        return song.tabs[instrument]
            ?.trim()
            ?.removePrefix("file:///android_asset/")
            ?.removePrefix("android_asset/")
            ?.removePrefix("assets/")
    }
}