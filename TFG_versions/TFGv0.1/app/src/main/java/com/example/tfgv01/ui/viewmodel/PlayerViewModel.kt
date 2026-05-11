package com.example.tfgv01.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.tfgv01.data.model.Cancion
import com.example.tfgv01.data.repository.SongRepository
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val repository = SongRepository()

    private val _searchResults = mutableStateOf<List<Cancion>>(emptyList())
    val searchResults: State<List<Cancion>> = _searchResults

    private val _selectedSong = mutableStateOf<Cancion?>(null)
    val selectedSong: State<Cancion?> = _selectedSong

    fun searchSongs(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.buscarCanciones(query)
        }
    }

    fun selectSong(cancion: Cancion) {
        _selectedSong.value = cancion
    }
}