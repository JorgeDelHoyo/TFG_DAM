// app/src/main/java/com/example/arpegio/ui/screens/LibraryScreen.kt
package com.example.arpegio.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.arpegio.data.model.Song
import com.example.arpegio.ui.viewmodel.LibraryUiEvent
import com.example.arpegio.ui.viewmodel.LibraryUiState
import com.example.arpegio.ui.viewmodel.LibraryViewModel

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
                val filteredLocalSongs = remember(state.localSongs, searchQuery) {
                    state.localSongs.filterByTitleOrArtist(searchQuery)
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        text = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                                               else "No hay canciones de la comunidad aún",
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

                    // Se pasan las canciones locales filtradas al componente
                    LocalSongsSection(
                        state = state,
                        filteredLocalSongs = filteredLocalSongs,
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
    filteredLocalSongs: List<Song>,
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
                    Text("Importar partitura local")
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredLocalSongs.isEmpty()) {
                    Text(
                        text = if (state.localSongs.isEmpty()) "Aún no has subido partituras personales."
                               else "Sin resultados en tus canciones locales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLocalSongs, key = { it.id }) { song ->
                            var menuExpanded by remember { mutableStateOf(false) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongClick(song) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Icono circular pequeño
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Archivo local",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )
                                        }
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
                    Text("Buscar partitura en el dispositivo")
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

/**
 * Tarjeta visual premium para cada canción de la comunidad.
 *
 * Muestra: icono circular con nota musical, título en negrita,
 * artista en color primario, chip de dificultad con código de color
 * (verde/naranja/rojo), lista de instrumentos disponibles, y flecha
 * de navegación indicando interactividad.
 */
@Composable
private fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Icono circular con nota musical ──
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Contenido central: título, artista, dificultad + instrumentos ──
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título destacado
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Artista — prominente con color primario
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fila de metadatos: dificultad + instrumentos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip de dificultad con color codificado
                    if (song.difficulty.isNotBlank()) {
                        DifficultyBadge(difficulty = song.difficulty)
                    }

                    // Instrumentos disponibles
                    if (song.tabs.isNotEmpty()) {
                        Text(
                            text = song.tabs.keys.joinToString(" · ") { instrument ->
                                instrument.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // ── Flecha de navegación ──
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir canción",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Badge compacto de dificultad con código de color:
 * - Verde (beginner) — para principiantes
 * - Naranja (intermediate) — nivel medio
 * - Rojo (advanced) — nivel avanzado
 */
@Composable
private fun DifficultyBadge(difficulty: String) {
    val (label, color) = when (difficulty.lowercase()) {
        "beginner" -> "Fácil" to Color(0xFF4CAF50)
        "intermediate" -> "Media" to Color(0xFFFFA726)
        "advanced" -> "Difícil" to Color(0xFFEF5350)
        else -> difficulty.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        } to MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontSize = 11.sp
        )
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
            "Carga temas en Firebase o importa tus partituras locales.",
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