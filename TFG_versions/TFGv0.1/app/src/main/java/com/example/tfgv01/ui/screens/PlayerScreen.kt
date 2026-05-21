// app/src/main/java/com/example/tfgv01/ui/screens/PlayerScreen.kt
package com.example.tfgv01.ui.screens

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.components.*
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
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    // 🚀 NUEVO ESTADO: Controla si la BottomBar está desplegada
    var isBottomBarExpanded by rememberSaveable { mutableStateOf(false) }
    val tabAssetPath = remember(song, selectedInstrument) {
        song.tabs[selectedInstrument]?.toAssetPath()
    }
    val partituraWebViewRef = remember { mutableStateOf<WebView?>(null) }
    var videoDuration by remember { mutableStateOf(180f) }

    // --- ⚡ MÓDULO DE FLUIDEZ ---
    var lastYoutubeTime by remember { mutableStateOf(0f) }
    var systemTimeAtLastUpdate by remember { mutableStateOf(0L) }
    LaunchedEffect(currentTime) {
        lastYoutubeTime = currentTime
        systemTimeAtLastUpdate = System.nanoTime()
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                withFrameMillis {
                    val now = System.nanoTime()
                    val elapsed = (now - systemTimeAtLastUpdate) / 1_000_000_000f
                    val extrapolatedTime = lastYoutubeTime + elapsed
                    partituraWebViewRef.value?.evaluateJavascript("correctAutoScrollTime($extrapolatedTime);", null)
                }
            }
        }
    }

    LaunchedEffect(song) { viewModel.loadSong(song) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 🔹 1. CABECERA Y SELECTOR (Se ocultan al expandir la barra inferior)
        AnimatedVisibility(
            visible = !isBottomBarExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                SongHeader(song = song, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                InstrumentSelector(
                    availableInstruments = song.tabs.keys.toList(),
                    selectedInstrument = selectedInstrument,
                    onInstrumentSelected = { viewModel.selectInstrument(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }

        // 🔹 2. ÁREA DE PARTITURA (Ocupa todo el centro)
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
                            if (isPlaying) webView.startAutoScroll(currentTime)
                        }
                    )
                }
            }
        }

        // 🔹 3. BOTTOM BAR DESPLEGABLE
        val barHeight by animateDpAsState(targetValue = if (isBottomBarExpanded) 140.dp else 48.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Tirador / Botón de control de despliegue (Siempre visible)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { isBottomBarExpanded = !isBottomBarExpanded }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isBottomBarExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expandir/Contraer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBottomBarExpanded) "Cerrar controles" else "Mostrar controles",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Contenido expandido (Solo se ve cuando isBottomBarExpanded es true)
                AnimatedVisibility(
                    visible = isBottomBarExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BLOQUE 1 (IZQUIERDA)
                        Column(
                            modifier = Modifier.wrapContentWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FilledTonalIconButton(onClick = { viewModel.toggleMute() }, modifier = Modifier.size(40.dp)) {
                                Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, Modifier.size(20.dp))
                            }
                            FilledTonalIconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // BLOQUE 2 (CENTRO)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⏱️ ${String.format("%.1f", currentTime)}s", style = MaterialTheme.typography.bodyMedium)
                            FloatingActionButton(
                                onClick = {
                                    viewModel.togglePlay()
                                    if (isPlaying) partituraWebViewRef.value?.stopAutoScroll()
                                    else partituraWebViewRef.value?.startAutoScroll(currentTime)
                                },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.3f))

                        // BLOQUE 3 (DERECHA)
                        YouTubePlayerSection(
                            videoId = song.youtubeVideoId,
                            isPlaying = isPlaying,
                            isMuted = isMuted,
                            onTimeUpdate = { viewModel.updateCurrentTime(it) },
                            onDurationUpdate = { videoDuration = it },
                            modifier = Modifier.width(160.dp).height(90.dp)
                        )
                    }
                }
            }
        }
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
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = song.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (song.difficulty.isNotBlank()) {
            val labelText = song.difficulty
            AssistChip(onClick = {}, label = { Text(labelText) }, modifier = Modifier.padding(top = 8.dp), enabled = false)
        }
    }
}

@Composable
private fun YouTubePlayerSection(
    videoId: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTimeUpdate: (Float) -> Unit,
    onDurationUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        YouTubePlayer(
            videoId = videoId,
            autoplay = false,
            onCurrentSecond = onTimeUpdate,
            onDurationReady = onDurationUpdate,
            externalControls = ExternalControls(
                play = isPlaying,
                playbackSpeed = 1.0f,
                isMuted = isMuted
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