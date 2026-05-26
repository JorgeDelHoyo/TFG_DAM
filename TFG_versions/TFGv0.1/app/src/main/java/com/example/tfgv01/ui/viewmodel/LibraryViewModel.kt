// app/src/main/java/com/example/tfgv01/ui/viewmodel/LibraryViewModel.kt
package com.example.tfgv01.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la interfaz de la pantalla de biblioteca.
 *
 * Sealed interface para representar los tres estados posibles:
 * carga, éxito con datos, o error con mensaje descriptivo.
 */
sealed interface LibraryUiState {
    /** Estado de carga inicial mientras se conectan los flujos de datos. */
    object Loading : LibraryUiState

    /**
     * Estado de éxito con las dos listas de canciones.
     *
     * @property remoteSongs Canciones de la comunidad (Firestore).
     * @property localSongs Canciones locales importadas (Room).
     * @property isLocalExpanded Si la sección de canciones locales está desplegada.
     */
    data class Success(
        val remoteSongs: List<Song>,
        val localSongs: List<Song>,
        val isLocalExpanded: Boolean = false
    ) : LibraryUiState

    /** Estado de error con mensaje para mostrar al usuario. */
    data class Error(val message: String) : LibraryUiState
}

/**
 * Eventos puntuales de la UI (one-shot events) que no deben persistir en el estado.
 * Se envían por [Channel] para garantizar que cada evento se consume exactamente una vez.
 */
sealed interface LibraryUiEvent {
    data class ShowToast(val message: String) : LibraryUiEvent
    object SongAddedSuccess : LibraryUiEvent
}

/**
 * ViewModel de la pantalla de biblioteca.
 *
 * Combina reactivamente los flujos de Firestore (canciones remotas) y Room (canciones locales)
 * usando [combine] para mantener la UI sincronizada con ambas fuentes de datos.
 *
 * @property repository Repositorio de canciones inyectado por Hilt.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LibraryUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadAllSongsCombined()
    }

    /**
     * Combina los flujos de Firestore y Room para tener una vista unificada.
     * Usa [combine] para re-emitir cada vez que cualquiera de las dos fuentes cambia.
     */
    private fun loadAllSongsCombined() {
        viewModelScope.launch {
            combine(
                repository.getSongs(),
                repository.getLocalSongs()
            ) { remote, local ->
                val currentExpanded = (_uiState.value as? LibraryUiState.Success)?.isLocalExpanded ?: false
                LibraryUiState.Success(
                    remoteSongs = remote,
                    localSongs = local,
                    isLocalExpanded = currentExpanded
                )
            }.catch { e ->
                _uiState.value = LibraryUiState.Error(
                    message = e.message ?: "Error al sincronizar las bibliotecas"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Elimina una canción local (archivo + registro de Room). */
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                repository.deleteLocalSong(song)
            } catch (e: Exception) {
                _uiEvent.send(LibraryUiEvent.ShowToast("Error al eliminar la canción"))
            }
        }
    }

    /** Actualiza el título de una canción local. */
    fun updateSongTitle(song: Song, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            try {
                val updatedSong = song.copy(title = newTitle)
                repository.updateLocalSong(updatedSong)
            } catch (e: Exception) {
                _uiEvent.send(LibraryUiEvent.ShowToast("Error al renombrar la canción"))
            }
        }
    }

    /** Alterna la visibilidad de la sección de canciones locales. */
    fun toggleLocalExpanded() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(isLocalExpanded = !currentState.isLocalExpanded)
        }
    }

    /**
     * Importa un archivo .gp3 seleccionado por el usuario.
     * Delega en [SongRepository.saveLocalSong] que copia el archivo y lo registra en Room.
     */
    fun addCustomSong(title: String, fileUri: Uri?) {
        if (title.isBlank() || fileUri == null) {
            viewModelScope.launch { _uiEvent.send(LibraryUiEvent.ShowToast("Campos inválidos")) }
            return
        }

        viewModelScope.launch {
            repository.saveLocalSong(title, fileUri)
                .onSuccess {
                    _uiEvent.send(LibraryUiEvent.SongAddedSuccess)
                }
                .onFailure { error ->
                    _uiEvent.send(LibraryUiEvent.ShowToast(error.localizedMessage ?: "Fallo al importar"))
                }
        }
    }

    /** Reinicia la carga de datos desde ambas fuentes. */
    fun refresh() {
        _uiState.value = LibraryUiState.Loading
        loadAllSongsCombined()
    }
}