package com.example.tfgv01.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.tfgv01.data.model.Cancion
import com.example.tfgv01.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(viewModel: PlayerViewModel, onSongSelected: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    // Lista temporal para pruebas (esto vendrá de Firestore)
    val cancionesMock = listOf(
        Cancion(id = "1", titulo = "Killer Queen", artista = "Queen", youtubeVideoId = "2ZBtPf7qPs4")
    )

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar canción o autor") }
        )

        LazyColumn {
            items(cancionesMock.filter { it.titulo.contains(searchQuery, ignoreCase = true) }) { cancion ->
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