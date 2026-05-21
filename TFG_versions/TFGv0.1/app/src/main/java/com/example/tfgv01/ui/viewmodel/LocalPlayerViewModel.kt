// app/src/main/java/com/example/tfgv01/ui/viewmodel/LocalPlayerViewModel.kt
package com.example.tfgv01.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfgv01.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalPlayerUiState(
    val isPlaying: Boolean = false,
    val currentTempoMultiplier: Float = 1.0f, // 1.0 = 100% velocidad original
    val selectedInstrument: String = "Guitarra",
    val availableInstruments: List<String> = emptyList(),
    val currentTimeSeconds: Float = 0f,       // 🆕 El segundero que moverá tu cursor maestro
    val totalDurationSeconds: Float = 180f    // Duración estimada de respaldo
)

@HiltViewModel
class LocalPlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LocalPlayerUiState())
    val uiState: StateFlow<LocalPlayerUiState> = _uiState.asStateFlow()

    private var metronomeJob: Job? = null
    private val timeStepMs = 50L // Frecuencia de actualización del cursor (50ms = 20 fps estables y fluidos)

    fun init(song: Song) {
        try {
            android.util.Log.d("LOCAL_BUG", "Iniciando canción local: ${song.title}")
            val instruments = song.tabs.keys.toList()
            android.util.Log.d("LOCAL_BUG", "Instrumentos detectados en el mapa: $instruments")

            _uiState.value = _uiState.value.copy(
                availableInstruments = instruments,
                selectedInstrument = instruments.firstOrNull() ?: "Guitarra"
            )
        } catch (e: Exception) {
            android.util.Log.e("LOCAL_BUG", "¡CRASH EN EL INIT DEL VIEWMODEL!", e)
        }
    }

    fun togglePlayPause() {
        val nextPlayingState = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = nextPlayingState)

        if (nextPlayingState) {
            startMetronomeTicker()
        } else {
            stopMetronomeTicker()
        }
    }

    private fun startMetronomeTicker() {
        metronomeJob?.cancel()
        metronomeJob = viewModelScope.launch {
            while (true) {
                delay(timeStepMs)
                val currentState = _uiState.value

                // Calculamos cuánto tiempo real ha avanzado multiplicando por el factor del Slider
                val secondsPassed = (timeStepMs / 1000f) * currentState.currentTempoMultiplier
                var newTime = currentState.currentTimeSeconds + secondsPassed

                // Si llega al final del tema estimado, vuelve al inicio (Loop)
                if (newTime >= currentState.totalDurationSeconds) {
                    newTime = 0f
                }

                _uiState.value = currentState.copy(currentTimeSeconds = newTime)
            }
        }
    }

    private fun stopMetronomeTicker() {
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun updateTempo(newMultiplier: Float) {
        val clampedValue = newMultiplier.coerceIn(0.5f, 2.0f)
        _uiState.value = _uiState.value.copy(currentTempoMultiplier = clampedValue)
    }

    // 🆕 Método crítico: Cuando AlphaTab termine de cargar la partitura local real,
    // nos enviará su duración nativa. Actualizamos el techo del segundero aquí.
    fun updateTotalDuration(duration: Float) {
        if (duration > 0f) {
            _uiState.value = _uiState.value.copy(totalDurationSeconds = duration)
        }
    }

    fun changeInstrument(instrument: String) {
        _uiState.value = _uiState.value.copy(selectedInstrument = instrument)
    }

    override fun onCleared() {
        super.onCleared()
        stopMetronomeTicker()
    }
}