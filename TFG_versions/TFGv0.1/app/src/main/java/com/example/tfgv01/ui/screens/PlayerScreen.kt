package com.example.tfgv01.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.tfgv01.ui.components.updateScrollPosition
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

    // Sincronizar cada vez que YouTube mande su pulso perezoso (baja frecuencia)
    LaunchedEffect(currentTime) {
        lastYoutubeTime = currentTime
        systemTimeAtLastUpdate = System.nanoTime()
    }

    // Reloj de alta frecuencia sincronizado con la tasa de refresco (60Hz / 120Hz)
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (true) {
                withFrameMillis { _ ->
                    val now = System.nanoTime()
                    // Calcular segundos reales del sistema transcurridos desde el último pulso de YT
                    val elapsedSystemSeconds = (now - systemTimeAtLastUpdate) / 1_000_000_000f

                    // Extrapolar el tiempo teniendo en cuenta la velocidad de reproducción
                    val extrapolatedTime = lastYoutubeTime + (elapsedSystemSeconds * playbackSpeed)

                    // Inyectar directamente al WebView sin pasar por recomposiciones de Compose
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        SongHeader(song = song, modifier = Modifier.padding(vertical = 8.dp))

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
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(240.dp)
        )

        PlaybackControls(
            isPlaying = isPlaying,
            currentTime = currentTime,
            playbackSpeed = playbackSpeed,
            onPlayPause = {
                // Calcular nuevo estado ANTES de toggle para evitar stale closure
                val wasPlaying = isPlaying
                viewModel.togglePlay()
                if (wasPlaying) {
                    partituraWebViewRef.value?.stopAutoScroll()
                } else {
                    partituraWebViewRef.value?.startAutoScroll(currentTime)
                }
            },
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        InstrumentSelector(
            availableInstruments = song.tabs.keys.toList(),
            selectedInstrument = selectedInstrument,
            onInstrumentSelected = { viewModel.selectInstrument(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        tabAssetPath?.let { assetPath ->
            val instrumentIndex = song.tabs.keys.toList().indexOf(selectedInstrument).coerceAtLeast(0)
            key(assetPath, instrumentIndex) {
                PartituraWebView(
                    urlArchivo = assetPath,
                    instrumentIndex = instrumentIndex,
                    videoDuration = videoDuration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .padding(vertical = 8.dp),
                    onWebViewCreated = { webView ->
                        partituraWebViewRef.value = webView
                        // Configurar el scroll inicial si el video está reproduciéndose
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
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun String.toAssetPath(): String = trim()
    .removePrefix("file:///android_asset/")
    .removePrefix("android_asset/")
    .removePrefix("assets/")

@Composable
private fun SongHeader(song: Song, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
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
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
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
private fun PlaybackControls(
    isPlaying: Boolean,
    currentTime: Float,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(text = "⏱️ ${String.format("%.1f", currentTime)}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(onClick = onPlayPause) {
                val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val description = if (isPlaying) "Pausar" else "Reproducir"
                Icon(imageVector = icon, contentDescription = description)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎚️ Velocidad:", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = playbackSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.25f..2.0f,
                    steps = 3,
                    modifier = Modifier.width(120.dp)
                )
                Text("${playbackSpeed}x", style = MaterialTheme.typography.labelMedium)
            }
        }
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
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
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
