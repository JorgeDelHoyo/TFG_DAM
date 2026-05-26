// app/src/main/java/com/example/tfgv01/ui/screens/LocalPlayerScreen.kt
package com.example.tfgv01.ui.screens

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.components.PartituraWebView
import com.example.tfgv01.ui.viewmodel.LocalPlayerViewModel
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlayerScreen(
    viewModel: LocalPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    song: Song
) {

    BackHandler(enabled = true) {
        onNavigateBack() // Ejecuta el cambio de estado hacia "library" en vez de salir de la app
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Controla si la BottomBar está desplegada
    var isBottomBarExpanded by rememberSaveable { mutableStateOf(false) }

    // Detectamos la orientación actual del terminal
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 🎹 LÓGICA DE BPM: Usamos el tempo real de la partitura
    val bpmActuales = (uiState.originalBPM * uiState.currentTempoMultiplier).toInt()

    // Inicializar el ViewModel de forma segura
    LaunchedEffect(song) {
        viewModel.init(song)
    }

    // 🛡️ BÚSQUEDA INTELIGENTE DE RUTA
    val currentLocalPath = remember(song, uiState.selectedInstrument) {
        song.tabs[uiState.selectedInstrument]
            ?: song.tabs.values.firstOrNull()
            ?: ""
    }

    val instrumentIndex = remember(uiState.availableInstruments, uiState.selectedInstrument) {
        uiState.availableInstruments.indexOf(uiState.selectedInstrument).coerceAtLeast(0)
    }

    val partituraWebViewRef = remember { mutableStateOf<WebView?>(null) }

    // 🎯 ACTUALIZACIÓN DE VELOCIDAD NATIVA
    LaunchedEffect(uiState.currentTempoMultiplier) {
        partituraWebViewRef.value?.evaluateJavascript(
            "setPlaybackSpeed(${uiState.currentTempoMultiplier});", null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .statusBarsPadding()
    ) {
        // 🔹 1. CABECERA Y SELECTOR
        AnimatedVisibility(
            visible = !isBottomBarExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                InstrumentSelectorLocal(
                    availableInstruments = uiState.availableInstruments,
                    selectedInstrument = uiState.selectedInstrument,
                    onInstrumentSelected = { viewModel.changeInstrument(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }

        // 🔹 2. ÁREA DE PARTITURA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            LaunchedEffect(currentLocalPath) {
                android.util.Log.d("RUTA_CRÍTICA", "El path que va al WebView es: '$currentLocalPath'")
            }

            if (currentLocalPath.isNotBlank()) {
                key(currentLocalPath, instrumentIndex) {
                    PartituraWebView(
                        urlArchivo = currentLocalPath,
                        instrumentIndex = instrumentIndex,
                        videoDuration = uiState.totalDurationSeconds,
                        esLocal = true,
                        modifier = Modifier.fillMaxSize(),
                        onWebViewCreated = { webView ->
                            partituraWebViewRef.value = webView

                            // Leer la duración real y BPM calculados por AlphaTab
                            webView.postDelayed({
                                webView.evaluateJavascript("window.totalDuration") { result ->
                                    val duration = result?.toFloatOrNull()
                                    if (duration != null && duration > 0f) {
                                        viewModel.updateTotalDuration(duration)
                                    }
                                }
                                webView.evaluateJavascript("window.scoreBPM") { result ->
                                    val bpm = result?.toIntOrNull()
                                    if (bpm != null && bpm > 0) {
                                        viewModel.updateOriginalBPM(bpm)
                                    }
                                }
                            }, 2500) // Esperar a que el score se cargue completamente
                        }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "La ruta del archivo está vacía en la base de datos", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 🔹 3. BOTTOM BAR DESPLEGABLE
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            val barTextColor = MaterialTheme.colorScheme.inverseOnSurface

            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 4.dp, bottom = if (isLandscape) 4.dp else 8.dp)
            ) {

                // Tirador de la barra
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
                            tint = barTextColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBottomBarExpanded) "Cerrar controles" else "Mostrar controles de tempo",
                            style = MaterialTheme.typography.labelLarge,
                            color = barTextColor
                        )
                    }
                }

                // Controles expandidos
                AnimatedVisibility(
                    visible = isBottomBarExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = barTextColor)
                            Spacer(modifier = Modifier.width(8.dp))

                            // 🚀 Muestra el tempo en BPM
                            Text(
                                text = "Tempo: $bpmActuales BPM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = barTextColor,
                                modifier = Modifier.width(110.dp)
                            )

                            Slider(
                                value = uiState.currentTempoMultiplier,
                                onValueChange = { viewModel.updateTempo(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp), tint = barTextColor)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Reproductor Nativo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = barTextColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.togglePlayPause()
                                        if (uiState.isPlaying) {
                                            partituraWebViewRef.value?.evaluateJavascript("pauseNative();", null)
                                        } else {
                                            partituraWebViewRef.value?.evaluateJavascript("playNative();", null)
                                        }
                                    },
                                    modifier = Modifier.size(50.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(26.dp))
                                }
                            }

                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongHeaderLocal(song: Song, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Archivo importado en almacenamiento local", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun InstrumentSelectorLocal(
    availableInstruments: List<String>,
    selectedInstrument: String,
    onInstrumentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableInstruments.size <= 1) return
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text("🎸 Pista seleccionada:", style = MaterialTheme.typography.labelMedium)
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