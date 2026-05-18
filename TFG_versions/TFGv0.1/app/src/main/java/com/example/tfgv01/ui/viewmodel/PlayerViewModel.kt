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

    // 🎵 Canción actual (se pasa desde LibraryScreen vía navegación)
    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    // 🎸 Instrumento seleccionado para la tablatura
    private val _selectedInstrument = MutableStateFlow<String>("guitar")
    val selectedInstrument: StateFlow<String> = _selectedInstrument.asStateFlow()

    // ▶️ Estado de reproducción (para sincronizar YouTube + WebView)
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // ⏱️ Tiempo actual del video (en segundos)
    private val _currentTime = MutableStateFlow(0f)
    val currentTime: StateFlow<Float> = _currentTime.asStateFlow()

    // 🎚️ Velocidad de reproducción (0.5x a 2.0x)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /**
     * 🎯 Carga la canción seleccionada desde LibraryScreen
     */
    fun loadSong(song: Song) {
        _song.value = song
        // Resetear estado al cargar nueva canción
        _isPlaying.value = false
        _currentTime.value = 0f
        _selectedInstrument.value = song.tabs.keys.firstOrNull() ?: "guitar"
    }

    /**
     * 🎸 Cambia el instrumento de la tablatura
     */
    fun selectInstrument(instrument: String) {
        if (_song.value?.hasTabFor(instrument) == true) {
            _selectedInstrument.value = instrument
        }
    }

    /**
     * ▶️/⏸️ Toggle play/pause
     */
    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    /**
     * ⏱️ Actualiza el tiempo actual del video (llamado desde YoutubePlayer)
     */
    fun updateCurrentTime(seconds: Float) {
        _currentTime.value = seconds
    }

    /**
     * ⏭️ Busca en el video (llamado desde UI o WebView)
     */
    fun seekTo(seconds: Float) {
        _currentTime.value = seconds
        // 🔜 Aquí podrías emitir un evento para que YoutubePlayer haga seek
    }

    /**
     * 🎚️ Cambia la velocidad de reproducción
     */
    fun setPlaybackSpeed(speed: Float) {
        // ✅ Redondear a los valores que YouTube acepta
        val validSpeed = when {
            speed <= 0.37f -> 0.25f
            speed <= 0.75f -> 0.5f
            speed <= 1.25f -> 1.0f
            speed <= 1.75f -> 1.5f
            else -> 2.0f
        }
        _playbackSpeed.value = validSpeed
    }

    /**
     * 🔄 Obtiene la URL del archivo .gp3 para el instrumento seleccionado
     */
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
