// app/src/main/java/com/example/tfgv01/ui/screens/LibraryScreen.kt
package com.example.tfgv01.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.viewmodel.LibraryUiState
import com.example.tfgv01.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongSelected: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    // 🔄 Observa el StateFlow del ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🎸 Mi Biblioteca") })
        }
    ) { padding ->
        when (val state = uiState) {

            is LibraryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LibraryUiState.Success -> {
                if (state.songs.isEmpty()) {
                    EmptyLibraryView(modifier = Modifier.padding(padding))
                } else {
                    SongList(
                        songs = state.songs,
                        onSongClick = onSongSelected,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            is LibraryUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// 📋 Lista de canciones con LazyColumn
@Composable
private fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(songs, key = { it.id }) { song ->
            SongItem(song = song, onClick = { onSongClick(song) })
        }
    }
}

// 🎵 Item individual de canción
@Composable
private fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Badge de dificultad (opcional)
            if (song.difficulty.isNotBlank()) {
                AssistChip(
                    onClick = {},
                    label = { Text(song.difficulty) },
                    modifier = Modifier.padding(top = 8.dp),
                    enabled = false
                )
            }
        }
    }
}

// 📭 Vista cuando no hay canciones
@Composable
private fun EmptyLibraryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎵 No hay canciones aún", style = MaterialTheme.typography.titleLarge)
        Text(
            "Añade tu primera tablatura desde Firebase Console",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ❌ Vista de error con botón de reintento
@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "❌ $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Reintentar")
        }
    }
}