package com.example.tfgv01.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.tfgv01.ui.components.ExternalControls  // ✅ Import crítico
import com.example.tfgv01.ui.viewmodel.PlayerViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener

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
    val tabAssetPath by remember(song, selectedInstrument) {
        derivedStateOf { viewModel.getTabAssetPath() }
    }

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
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        }

        SongHeader(song = song, modifier = Modifier.padding(vertical = 8.dp))

        YouTubePlayerSection(
            videoId = song.youtubeVideoId,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            onTimeUpdate = { seconds -> viewModel.updateCurrentTime(seconds) },
            onPlayerReady = { youTubePlayer ->
                youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        viewModel.updateCurrentTime(second)
                    }
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        )

        PlaybackControls(
            isPlaying = isPlaying,
            currentTime = currentTime,
            playbackSpeed = playbackSpeed,
            onPlayPause = { viewModel.togglePlay() },
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
            val instrumentIndex = song.tabs.keys.toList().indexOf(selectedInstrument)
            PartituraWebView(
                urlArchivo = assetPath,
                instrumentIndex = instrumentIndex
            )
        } ?: run {
            Text(
                text = "❌ No hay tablatura disponible para ${selectedInstrument}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

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
    onPlayerReady: (YouTubePlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        YouTubePlayer(
            videoId = videoId,
            autoplay = false,
            onPlayerReady = onPlayerReady,
            externalControls = ExternalControls(play = isPlaying, playbackSpeed = playbackSpeed)
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
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            FloatingActionButton(onClick = onPlayPause) {
                val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val description = if (isPlaying) "Pausar" else "Reproducir"
                Icon(imageVector = icon, contentDescription = description)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎚️ Velocidad:", style = MaterialTheme.typography.labelMedium)
                Slider(value = playbackSpeed, onValueChange = onSpeedChange, valueRange = 0.5f..2.0f, steps = 5, modifier = Modifier.width(120.dp))
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
            availableInstruments.forEachIndexed { index, instrument ->
                Tab(selected = instrument == selectedInstrument, onClick = { onInstrumentSelected(instrument) }, text = { Text(instrument.capitalize()) })
            }
        }
    }
}