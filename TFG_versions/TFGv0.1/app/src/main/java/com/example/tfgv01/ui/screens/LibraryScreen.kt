// app/src/main/java/com/example/tfgv01/ui/screens/LibraryScreen.kt
package com.example.tfgv01.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.viewmodel.LibraryUiEvent
import com.example.tfgv01.ui.viewmodel.LibraryUiState
import com.example.tfgv01.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongSelected: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Estados internos para controlar el diálogo de edición de nombre
    var songToEdit by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LibraryUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                LibraryUiEvent.SongAddedSuccess -> {
                    showAddDialog = false
                    Toast.makeText(context, "¡Partitura añadida con éxito!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("🎸 Mi Biblioteca") })
        }
    ) { padding ->
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LibraryUiState.Success -> {
                val filteredRemoteSongs = remember(state.remoteSongs, searchQuery) {
                    state.remoteSongs.filterByTitleOrArtist(searchQuery)
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (state.remoteSongs.isEmpty() && state.localSongs.isEmpty()) {
                        EmptyLibraryView(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Canciones de la Comunidad",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            if (filteredRemoteSongs.isEmpty()) {
                                item {
                                    Text(
                                        text = "No hay canciones globales coincidentes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            items(filteredRemoteSongs, key = { it.id }) { song ->
                                SongItem(song = song, onClick = { onSongSelected(song) })
                            }
                        }
                    }

                    // Se pasan las nuevas funciones lambda al componente de canciones locales
                    LocalSongsSection(
                        state = state,
                        onHeaderClick = viewModel::toggleLocalExpanded,
                        onAddSongClick = { showAddDialog = true },
                        onSongClick = onSongSelected,
                        onEditSongClick = { song -> songToEdit = song },
                        onDeleteSongClick = { song -> viewModel.deleteSong(song) }
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

    if (showAddDialog) {
        AddSongDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, uri -> viewModel.addCustomSong(title, uri) }
        )
    }

    // Diálogo emergente para editar nombre si hay una canción seleccionada para tal fin
    songToEdit?.let { song ->
        EditSongDialog(
            song = song,
            onDismiss = { songToEdit = null },
            onConfirm = { newTitle ->
                viewModel.updateSongTitle(song, newTitle)
                songToEdit = null
            }
        )
    }
}

@Composable
private fun LocalSongsSection(
    state: LibraryUiState.Success,
    onHeaderClick: () -> Unit,
    onAddSongClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onEditSongClick: (Song) -> Unit,
    onDeleteSongClick: (Song) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis Canciones Propias (${state.localSongs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (state.isLocalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (state.isLocalExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddSongClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar archivo .gp3 local")
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (state.localSongs.isEmpty()) {
                    Text(
                        text = "Aún no has subido partituras personales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.localSongs, key = { it.id }) { song ->
                            var menuExpanded by remember { mutableStateOf(false) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongClick(song) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = song.title, style = MaterialTheme.typography.bodyLarge)
                                    }

                                    // Menú de opciones contextual flotante de 3 puntos (Editar/Eliminar)
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Opciones de canción propia"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Editar nombre") },
                                                onClick = {
                                                    menuExpanded = false
                                                    onEditSongClick(song)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onDeleteSongClick(song)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(song.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Título de la Canción") },
        text = {
            Column {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Título del Tema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTitle) },
                enabled = newTitle.isNotBlank() && newTitle != song.title
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AddSongDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Ningún archivo cargado") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        fileName = uri?.lastPathSegment ?: "Archivo seleccionado"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Partitura Propia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Tema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buscar .gp3 en el dispositivo")
                }
                Text(text = fileName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedUri) },
                enabled = title.isNotBlank() && selectedUri != null
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Buscar por título o artista") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                }
            }
        }
    )
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = song.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun EmptyLibraryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎵 No hay canciones aún", style = MaterialTheme.typography.titleLarge)
        Text(
            "Carga temas en Firebase o importa tus .gp3 locales.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌ $message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Reintentar")
        }
    }
}

private fun List<Song>.filterByTitleOrArtist(query: String): List<Song> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { song ->
        song.title.contains(normalizedQuery, ignoreCase = true) ||
                song.artist.contains(normalizedQuery, ignoreCase = true)
    }
}