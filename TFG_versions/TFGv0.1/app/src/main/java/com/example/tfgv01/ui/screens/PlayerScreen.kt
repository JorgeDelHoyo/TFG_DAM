package com.example.tfgv01.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.components.PartituraWebView
import com.example.tfgv01.ui.components.YouTubePlayer
import com.example.tfgv01.ui.components.ExternalControls
import com.example.tfgv01.ui.components.correctAutoScrollTime
import com.example.tfgv01.ui.components.startAutoScroll
import com.example.tfgv01.ui.components.stopAutoScroll
import com.example.tfgv01.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    song: Song
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val tabAssetPath = remember(song, selectedInstrument) {
        song.tabs[selectedInstrument]?.toAssetPath()
    }

    val partituraWebViewRef = remember { mutableStateOf<WebView?>(null) }
    var videoDuration by remember { mutableStateOf(180f) }

    // --- ⚡ MÓDULO DE FLUIDEZ ABSOLUTA ⚡ ---
    var lastYoutubeTime by remember { mutableStateOf(0f) }
    var systemTimeAtLastUpdate by remember { mutableStateOf(0L) }

    LaunchedEffect(currentTime) {
        lastYoutubeTime = currentTime
        systemTimeAtLastUpdate = System.nanoTime()
    }

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (true) {
                withFrameMillis { _ ->
                    val now = System.nanoTime()
                    val elapsedSystemSeconds = (now - systemTimeAtLastUpdate) / 1_000_000_000f
                    val extrapolatedTime = lastYoutubeTime + (elapsedSystemSeconds * playbackSpeed)
                    partituraWebViewRef.value?.evaluateJavascript(
                        "correctAutoScrollTime($extrapolatedTime);",
                        null
                    )
                }
            }
        }
    }
    // ----------------------------------------

    LaunchedEffect(song) {
        viewModel.loadSong(song)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars) // Blindaje contra la barra nativa de Android
    ) {
        // --- CAPA INFERIOR: Interfaz Principal ---
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cabecera de la canción arriba (ahora sin el botón volver estorbando)
            SongHeader(song = song, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

            // Selector de instrumento justo bajo la cabecera
            InstrumentSelector(
                availableInstruments = song.tabs.keys.toList(),
                selectedInstrument = selectedInstrument,
                onInstrumentSelected = { viewModel.selectInstrument(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            // LA PARTITURA: Ahora se expande muchísimo más al no tener elementos arriba ni abajo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                tabAssetPath?.let { assetPath ->
                    val instrumentIndex = song.tabs.keys.toList().indexOf(selectedInstrument).coerceAtLeast(0)
                    key(assetPath, instrumentIndex) {
                        PartituraWebView(
                            urlArchivo = assetPath,
                            instrumentIndex = instrumentIndex,
                            videoDuration = videoDuration,
                            modifier = Modifier.fillMaxSize(),
                            onWebViewCreated = { webView ->
                                partituraWebViewRef.value = webView
                                if (isPlaying) {
                                    webView.startAutoScroll(currentTime)
                                }
                            }
                        )
                    }
                } ?: run {
                    Text(
                        text = "No hay tablatura disponible para ${selectedInstrument}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp).align(Alignment.Center)
                    )
                }
            }

            // NUEVA BOTTOM BAR GENEROSA Y MODERNA
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp, // Le da un tono grisáceo/elevado muy elegante acorde a Material 3
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Fila 1 de la barra: Tiempo y Control de Velocidad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⏱️ ${String.format("%.1f", currentTime)}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎚️ Velocidad:", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { viewModel.setPlaybackSpeed(it) },
                                valueRange = 0.25f..2.0f,
                                steps = 3,
                                modifier = Modifier.width(130.dp).padding(horizontal = 4.dp)
                            )
                            Text("${playbackSpeed}x", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fila 2 de la barra: Botón Volver, Botón Play y hueco para el Vídeo Flotante
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón de Volver (Esquina izquierda de la barra)
                        FilledTonalIconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }

                        Spacer(modifier = Modifier.weight(0.4f)) // Empuja el Play al centro relativo

                        // Botón de Play/Pause (Centro de la barra)
                        FloatingActionButton(
                            onClick = {
                                val wasPlaying = isPlaying
                                viewModel.togglePlay()
                                if (wasPlaying) {
                                    partituraWebViewRef.value?.stopAutoScroll()
                                } else {
                                    partituraWebViewRef.value?.startAutoScroll(currentTime)
                                }
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                            Icon(imageVector = icon, contentDescription = "Play/Pause", modifier = Modifier.size(28.dp))
                        }

                        // Espacio en blanco reservado a la derecha de la barra (weight)
                        // Aquí es exactamente donde se posará visualmente la ventana flotante de YT
                        Spacer(modifier = Modifier.weight(1.2f))
                    }
                }
            }
        }

        // --- CAPA SUPERIOR: Ventana flotante de YouTube perfectamente acoplada ---
        YouTubePlayerSection(
            videoId = song.youtubeVideoId,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            onTimeUpdate = { seconds ->
                viewModel.updateCurrentTime(seconds)
                partituraWebViewRef.value?.correctAutoScrollTime(seconds)
            },
            onDurationUpdate = { duration ->
                videoDuration = duration
                partituraWebViewRef.value?.evaluateJavascript("if (typeof window.setVideoDuration === 'function') { setVideoDuration($duration); }", null)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd) // Lo ancla abajo a la derecha
                .padding(bottom = 8.dp, end = 12.dp) // Alineado limpiamente dentro del margen de la Bottom Bar
                .width(150.dp)  // Tamaño ideal tipo miniatura de esquina
                .height(84.dp)  // Relación de aspecto 16:9 exacta
        )
    }
}

private fun String.toAssetPath(): String = trim()
    .removePrefix("file:///android_asset/")
    .removePrefix("android_asset/")
    .removePrefix("assets/")

@Composable
private fun SongHeader(song: Song, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding() // 👈 Evita que el título se meta debajo de la hora/batería del móvil
            .padding(horizontal = 16.dp, vertical = 8.dp) // Le da un aire extra muy limpio
    ) {
        Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = song.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (song.difficulty.isNotBlank()) {
            AssistChip(onClick = {}, label = { Text(song.difficulty) }, modifier = Modifier.padding(top = 8.dp), enabled = false)
        }
    }
}

@Composable
private fun YouTubePlayerSection(
    videoId: String,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onTimeUpdate: (Float) -> Unit,
    onDurationUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp) // Sombra pronunciada para destacar la flotabilidad sobre la barra
    ) {
        YouTubePlayer(
            videoId = videoId,
            autoplay = false,
            onCurrentSecond = onTimeUpdate,
            onDurationReady = onDurationUpdate,
            externalControls = ExternalControls(
                play = isPlaying,
                playbackSpeed = playbackSpeed
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun InstrumentSelector(
    availableInstruments: List<String>,
    selectedInstrument: String,
    onInstrumentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableInstruments.size <= 1) return
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text("🎸 Instrumento:", style = MaterialTheme.typography.labelMedium)
        ScrollableTabRow(selectedTabIndex = availableInstruments.indexOf(selectedInstrument), edgePadding = 0.dp, modifier = Modifier.padding(top = 4.dp)) {
            availableInstruments.forEach { instrument ->
                Tab(
                    selected = instrument == selectedInstrument,
                    onClick = { onInstrumentSelected(instrument) },
                    text = { Text(instrument.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                )
            }
        }
    }
}