package com.example.tfgv01.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.tfgv01.data.model.Cancion
import com.example.tfgv01.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(viewModel: PlayerViewModel, onSongSelected: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    // IMPORTANTE: Observamos la lista real que viene de Firebase a través del ViewModel
    val canciones by viewModel.songs

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar canción o autor") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn {
            // Filtramos sobre la lista REAL de Firebase
            items(canciones.filter {
                it.titulo.contains(searchQuery, ignoreCase = true) ||
                        it.artista.contains(searchQuery, ignoreCase = true)
            }) { cancion ->
                ListItem(
                    headlineContent = { Text(cancion.titulo) },
                    supportingContent = { Text(cancion.artista) },
                    modifier = Modifier.clickable {
                        viewModel.selectSong(cancion)
                        onSongSelected()
                    }
                )
            }
        }
    }
}
