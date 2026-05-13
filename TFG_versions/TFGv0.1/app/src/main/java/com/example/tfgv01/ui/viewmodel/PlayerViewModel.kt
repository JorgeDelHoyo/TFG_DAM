package com.example.tfgv01.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfgv01.data.model.Cancion
import com.example.tfgv01.data.repository.SongRepository
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val repository = SongRepository()

    private val _songs = mutableStateOf<List<Cancion>>(emptyList())
    val songs: State<List<Cancion>> = _songs

    private val _selectedSong = mutableStateOf<Cancion?>(null)
    val selectedSong: State<Cancion?> = _selectedSong

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            val remoteSongs = repository.getSongs()
            _songs.value = remoteSongs
        }
    }

    fun selectSong(cancion: Cancion?) {
        _selectedSong.value = cancion
    }
}