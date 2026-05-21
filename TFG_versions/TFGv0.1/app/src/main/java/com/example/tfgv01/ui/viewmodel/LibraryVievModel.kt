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

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(
        val remoteSongs: List<Song>,
        val localSongs: List<Song>,
        val isLocalExpanded: Boolean = false
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

sealed interface LibraryUiEvent {
    data class ShowToast(val message: String) : LibraryUiEvent
    object SongAddedSuccess : LibraryUiEvent
}

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

    private fun loadAllSongsCombined() {
        viewModelScope.launch {
            // Combinamos reactivamente el flujo de Firebase y el flujo de Room
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

    fun toggleLocalExpanded() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(isLocalExpanded = !currentState.isLocalExpanded)
        }
    }

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

    fun refresh() {
        _uiState.value = LibraryUiState.Loading
        loadAllSongsCombined()
    }
}