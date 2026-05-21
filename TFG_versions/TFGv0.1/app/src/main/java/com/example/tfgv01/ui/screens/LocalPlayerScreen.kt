// app/src/main/java/com/example/tfgv01/ui/screens/LocalPlayerScreen.kt
package com.example.tfgv01.ui.screens

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.components.PartituraWebView
import com.example.tfgv01.ui.viewmodel.LocalPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlayerScreen(
    viewModel: LocalPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    song: Song
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Controla si la BottomBar está desplegada
    var isBottomBarExpanded by rememberSaveable { mutableStateOf(false) }

    // Detectamos la orientación actual del terminal
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Inicializar el ViewModel de forma segura
    LaunchedEffect(song) {
        viewModel.init(song)
    }

    // 🛡️ BÚSQUEDA INTELIGENTE DE RUTA: Intenta usar el instrumento del estado.
    // Si no coincide o está vacío al arrancar, toma la primera ruta física que haya en el mapa como salvavidas.
    val currentLocalPath = remember(song, uiState.selectedInstrument) {
        song.tabs[uiState.selectedInstrument]
            ?: song.tabs.values.firstOrNull()
            ?: ""
    }

    // Calculamos dinámicamente el índice real del instrumento para pasárselo de forma segura a AlphaTab
    val instrumentIndex = remember(uiState.availableInstruments, uiState.selectedInstrument) {
        uiState.availableInstruments.indexOf(uiState.selectedInstrument).coerceAtLeast(0)
    }

    val partituraWebViewRef = remember { mutableStateOf<WebView?>(null) }

    // 🎯 TRANSPONSOR DE TIEMPO SEGURO (Sin bucles infinitos de frames)
    // Cada vez que el Ticker del ViewModel actualice los segundos, se los inyectamos directamente al JS
    LaunchedEffect(uiState.currentTimeSeconds) {
        if (uiState.isPlaying) {
            partituraWebViewRef.value?.evaluateJavascript(
                "correctAutoScrollTime(${uiState.currentTimeSeconds});", null
            )
        }
    }

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
                SongHeaderLocal(song = song, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
            // 🔍 Imprimimos la ruta en el Logcat para cazar el formato exacto
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

                            webView.postDelayed({
                                webView.evaluateJavascript("if(typeof totalDuration !== 'undefined') totalDuration") { result ->
                                    val duration = result?.toFloatOrNull() ?: 60f
                                    viewModel.updateTotalDuration(duration)
                                }
                            }, 1000)
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
        val targetHeight = if (isBottomBarExpanded) (if (isLandscape) 110.dp else 155.dp) else 48.dp
        val barHeight by animateDpAsState(targetValue = targetHeight)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

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
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBottomBarExpanded) "Cerrar controles" else "Mostrar controles de tempo",
                            style = MaterialTheme.typography.labelLarge
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
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Velocidad: ${(uiState.currentTempoMultiplier * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(95.dp)
                            )
                            Slider(
                                value = uiState.currentTempoMultiplier,
                                onValueChange = { viewModel.updateTempo(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalIconButton(onClick = onNavigateBack, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "⏱️ ${String.format("%.1f", uiState.currentTimeSeconds)}s / ${uiState.totalDurationSeconds.toInt()}s",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.togglePlayPause()
                                        if (uiState.isPlaying) {
                                            partituraWebViewRef.value?.evaluateJavascript("stopAutoScroll();", null)
                                        } else {
                                            partituraWebViewRef.value?.evaluateJavascript("startAutoScroll(${uiState.currentTimeSeconds});", null)
                                        }
                                    },
                                    modifier = Modifier.size(50.dp)
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