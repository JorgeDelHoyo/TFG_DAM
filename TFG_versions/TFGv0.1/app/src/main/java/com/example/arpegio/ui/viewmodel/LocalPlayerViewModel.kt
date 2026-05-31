package com.example.arpegio.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.arpegio.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Estado UI de la pantalla del reproductor local (canciones locales importadas por el usuario).
 *
 * @property isPlaying Indica si el metrónomo virtual está avanzando.
 * @property currentTempoMultiplier Multiplicador de velocidad (1.0 = 100% original, 0.5 = mitad, 2.0 = doble).
 * @property selectedInstrument Nombre del instrumento/pista actualmente seleccionado.
 * @property availableInstruments Lista de instrumentos disponibles en la partitura cargada.
 * @property currentTimeSeconds Tiempo actual del metrónomo virtual en segundos.
 * @property totalDurationSeconds Duración total estimada de la partitura en segundos.
 */
data class LocalPlayerUiState(
    val isPlaying: Boolean = false,
    val currentTempoMultiplier: Float = 1.0f,
    val selectedInstrument: String = "Guitarra",
    val availableInstruments: List<String> = emptyList(),
    val currentTimeSeconds: Float = 0f,
    val totalDurationSeconds: Float = 180f,
    val originalBPM: Int = 120
)

/**
 * ViewModel para el reproductor de canciones locales (archivos .gp3 importados).
 *
 * A diferencia del PlayerViewModel (que se sincroniza con YouTube), este ViewModel
 * genera su propio reloj interno (metrónomo) que avanza el cursor de la partitura.
 * El usuario puede ajustar la velocidad con el slider de tempo.
 */
@HiltViewModel
class LocalPlayerViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "LocalPlayerVM"
        /** Frecuencia de actualización del cursor: 50ms = 20 fps. */
        private const val TIME_STEP_MS = 50L
    }

    private val _uiState = MutableStateFlow(LocalPlayerUiState())
    val uiState: StateFlow<LocalPlayerUiState> = _uiState.asStateFlow()

    /**
     * Inicializa el ViewModel con los datos de la canción seleccionada.
     * Extrae los instrumentos disponibles del mapa de tablaturas.
     */
    fun init(song: Song) {
        try {
            val instruments = song.tabs.keys.toList()
            Log.d(TAG, "Canción local cargada: '${song.title}' — Instrumentos: $instruments")

            _uiState.value = _uiState.value.copy(
                availableInstruments = instruments,
                selectedInstrument = instruments.firstOrNull() ?: "Guitarra"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar canción local", e)
        }
    }

    /**
     * Alterna entre reproducción y pausa del reproductor nativo de AlphaTab.
     */
    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    /**
     * Actualiza el multiplicador de velocidad del metrónomo.
     * @param newMultiplier Valor entre 0.5 (50%) y 2.0 (200%).
     */
    fun updateTempo(newMultiplier: Float) {
        val clampedValue = newMultiplier.coerceIn(0.5f, 2.0f)
        _uiState.value = _uiState.value.copy(currentTempoMultiplier = clampedValue)
    }

    /**
     * Recibe la duración real de la partitura calculada por AlphaTab.
     * Llamado desde el bridge JavaScript una vez AlphaTab termina de renderizar.
     *
     * @param duration Duración en segundos según el score de AlphaTab.
     */
    fun updateTotalDuration(duration: Float) {
        if (duration > 0f) {
            _uiState.value = _uiState.value.copy(totalDurationSeconds = duration)
        }
    }

    /** Actualiza el BPM original del score (leído desde AlphaTab). */
    fun updateOriginalBPM(bpm: Int) {
        if (bpm > 0) {
            _uiState.value = _uiState.value.copy(originalBPM = bpm)
            Log.d(TAG, "BPM original del score: $bpm")
        }
    }

    /** Cambia el instrumento/pista seleccionado. */
    fun changeInstrument(instrument: String) {
        _uiState.value = _uiState.value.copy(selectedInstrument = instrument)
    }

    override fun onCleared() {
        super.onCleared()
    }
}