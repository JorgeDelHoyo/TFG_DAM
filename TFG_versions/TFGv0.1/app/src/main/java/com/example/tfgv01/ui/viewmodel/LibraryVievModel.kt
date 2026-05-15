// app/src/main/java/com/example/tfgv01/ui/viewmodel/LibraryViewModel.kt
package com.example.tfgv01.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// 🎯 Estados posibles de la UI (patrón UI State)
sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(val songs: List<Song>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {

    // 📡 Estado público: la UI observa este Flow
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadSongs()
    }

    // 🔁 Carga canciones desde Firestore
    private fun loadSongs() {
        viewModelScope.launch {
            repository.getSongs()
                .catch { e ->
                    // Si hay error, emitimos estado Error
                    _uiState.value = LibraryUiState.Error(
                        message = e.message ?: "Error desconocido al cargar canciones"
                    )
                }
                .collect { songs ->
                    // Cuando llegan datos, emitimos estado Success
                    _uiState.value = LibraryUiState.Success(songs)
                }
        }
    }

    // 🔄 Método público para refrescar manualmente (pull-to-refresh, retry, etc.)
    fun refresh() {
        _uiState.value = LibraryUiState.Loading
        loadSongs()
    }
}