// app/src/main/java/com/example/tfgv01/ui/screens/PlayerScreen.kt
package com.example.tfgv01.ui.screens

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.components.*
import com.example.tfgv01.ui.viewmodel.PlayerViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    song: Song
) {
    BackHandler(enabled = true) {
        onNavigateBack() // Ejecuta el cambio de estado hacia "library" en vez de salir de la app
    }

    val selectedInstrument by viewModel.selectedInstrument.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val syncOffset by viewModel.syncOffset.collectAsStateWithLifecycle()

    // Controla si la BottomBar está desplegada
    var isBottomBarExpanded by rememberSaveable { mutableStateOf(false) }

    // Detectamos de forma reactiva la orientación actual del terminal
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                    val extrapolatedTime = lastYoutubeTime + elapsed + syncOffset
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
            .statusBarsPadding()
    ) {
        // 🔹 1. CABECERA Y SELECTOR (Se ocultan al expandir la barra inferior)
        AnimatedVisibility(
            visible = !isBottomBarExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
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
                            if (isPlaying) webView.startAutoScroll(currentTime + syncOffset)
                        }
                    )
                }
            }
        }

        // 🔹 3. BOTTOM BAR DESPLEGABLE Y RESPONSIVA
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 4.dp, bottom = if (isLandscape) 4.dp else 8.dp)
            ) {

                // Tirador (Siempre visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { isBottomBarExpanded = !isBottomBarExpanded }
                        .padding(horizontal = 16.dp),
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

                // Contenido expandido
                AnimatedVisibility(
                    visible = isBottomBarExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Botón Volver
                        FilledTonalIconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(if (isLandscape) 48.dp else 44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp))
                        }

                        // Botón Mute/Unmute
                        FilledTonalIconButton(
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier.size(if (isLandscape) 48.dp else 44.dp)
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                "Volumen",
                                Modifier.size(22.dp)
                            )
                        }

                        // Timer + Play/Pause
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "⏱️ ${String.format("%.1f", currentTime)}s",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            FloatingActionButton(
                                onClick = {
                                    viewModel.togglePlay()
                                    if (isPlaying) partituraWebViewRef.value?.stopAutoScroll()
                                    else partituraWebViewRef.value?.startAutoScroll(currentTime + syncOffset)
                                },
                                modifier = Modifier.size(if (isLandscape) 48.dp else 50.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    "Play/Pausa",
                                    Modifier.size(24.dp)
                                )
                            }
                        }

                        // Offset Sync Controls (Sincronización manual)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sincronizar", style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = { viewModel.adjustSyncOffset(-0.5f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${if (syncOffset > 0) "+" else ""}${String.format("%.1f", syncOffset)}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { viewModel.adjustSyncOffset(0.5f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 4. YOUTUBE PLAYER — Siempre vivo, INVISIBLE (1dp)
        // Solo existe para proveer audio y time updates desde YouTube.
        // No necesita ser visible: la partitura + cursor son la interfaz principal.
        Box(modifier = Modifier.size(1.dp)) {
            YouTubePlayerSection(
                videoId = song.youtubeVideoId,
                isPlaying = isPlaying,
                isMuted = isMuted,
                onTimeUpdate = { seconds ->
                    viewModel.updateCurrentTime(seconds)
                    partituraWebViewRef.value?.correctAutoScrollTime(seconds + syncOffset)
                },
                onDurationUpdate = { duration ->
                    videoDuration = duration
                    partituraWebViewRef.value?.evaluateJavascript("if (typeof window.setVideoDuration === 'function') { setVideoDuration($duration); }", null)
                },
                modifier = Modifier.fillMaxSize()
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