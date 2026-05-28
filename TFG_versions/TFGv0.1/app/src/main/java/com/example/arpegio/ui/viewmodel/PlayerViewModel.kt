// app/src/main/java/com/example/tfgv01/ui/viewmodel/PlayerViewModel.kt
package com.example.arpegio.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.arpegio.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel para la pantalla de reproductor principal (canciones de Firebase + YouTube).
 *
 * Gestiona el estado de reproducción del vídeo de YouTube sincronizado con la partitura.
 * El [SavedStateHandle] permite restaurar estado tras la destrucción del proceso.
 *
 * @property savedStateHandle handle inyectado por Hilt para persistir estado ante config changes.
 */
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

    /** Estado de silenciamiento del audio del vídeo de YouTube. */
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    /** Offset manual de sincronización en segundos para ajustar el cursor con el vídeo. */
    private val _syncOffset = MutableStateFlow(0f)
    val syncOffset: StateFlow<Float> = _syncOffset.asStateFlow()

    /**
     * Carga una canción y reinicia todo el estado de reproducción.
     * Selecciona el primer instrumento disponible en las tablaturas de la canción.
     */
    fun loadSong(song: Song) {
        _song.value = song
        _isPlaying.value = false
        _currentTime.value = 0f
        _isMuted.value = false
        _syncOffset.value = 0f // Resetear el offset al cambiar de canción
        _selectedInstrument.value = song.tabs.keys.firstOrNull() ?: "guitar"
    }

    /**
     * Cambia el instrumento/pista seleccionado, si la canción tiene tablatura para él.
     * Provoca un re-render de la partitura en el WebView.
     */
    fun selectInstrument(instrument: String) {
        if (_song.value?.hasTabFor(instrument) == true) {
            _selectedInstrument.value = instrument
        }
    }

    /** Alterna entre play y pausa del vídeo de YouTube. */
    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    /** Alterna entre muteado y desmuteado del audio. */
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    /**
     * Actualiza el tiempo actual de reproducción del vídeo.
     * Llamado desde el bridge de JavaScript del reproductor de YouTube (~2 veces/seg).
     */
    fun updateCurrentTime(seconds: Float) {
        _currentTime.value = seconds
    }

    /** Salta a un segundo específico del vídeo (seek). */
    fun seekTo(seconds: Float) {
        _currentTime.value = seconds
    }

    /** Ajusta el offset de sincronización en la cantidad de segundos indicada (+/-). */
    fun adjustSyncOffset(deltaSeconds: Float) {
        _syncOffset.value += deltaSeconds
    }

    /**
     * Obtiene la ruta del asset de tablatura para el instrumento seleccionado,
     * limpiando prefijos innecesarios que podrían venir de Firebase.
     *
     * @return Ruta relativa del archivo .gp3 dentro de /assets/, o null si no hay tab.
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