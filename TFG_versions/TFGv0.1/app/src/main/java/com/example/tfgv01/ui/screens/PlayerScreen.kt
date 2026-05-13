package com.example.tfgv01.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tfgv01.data.model.PartituraRelacion
import com.example.tfgv01.ui.components.PartituraWebView
import com.example.tfgv01.ui.components.YoutubePlayer
import com.example.tfgv01.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val cancion by viewModel.selectedSong
    var instrumentoSeleccionado by remember { mutableStateOf<PartituraRelacion?>(null) }

    // Log para saber qué está llegando del ViewModel
    LaunchedEffect(cancion) {
        android.util.Log.d("YOUTUBE_CHECK", "Cancion: ${cancion?.titulo} | URL: ${cancion?.link}")
        if (instrumentoSeleccionado == null && !cancion?.partituras.isNullOrEmpty()) {
            instrumentoSeleccionado = cancion?.partituras?.first()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. REPRODUCTOR DE YOUTUBE
        cancion?.let { currentSong ->
            // Usamos la función externa para limpiar el ID
            val videoId = remember(currentSong.link) {
                extraerIdDeYoutube(currentSong.link)
            }

            if (videoId.length == 11) {
                YoutubePlayer(videoId = videoId)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("ID de vídeo no válido: '$videoId'", color = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. SELECTOR DE INSTRUMENTO
        Text(
            text = "Selecciona tu instrumento:",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            cancion?.partituras?.forEach { partitura ->
                FilterChip(
                    selected = instrumentoSeleccionado == partitura,
                    onClick = { instrumentoSeleccionado = partitura },
                    label = { Text(partitura.instrumentoId) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 3. VISOR DE PARTITURA
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (instrumentoSeleccionado != null) {
                val index = cancion?.partituras?.indexOf(instrumentoSeleccionado) ?: 0
                PartituraWebView(
                    urlArchivo = instrumentoSeleccionado!!.archivo,
                    instrumentIndex = index
                )
            }
        }
    }
}

// Función de limpieza fuera del Composable
fun extraerIdDeYoutube(url: String): String {
    if (url.isBlank()) return ""
    return try {
        val id = when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfterLast("/").substringBefore("?")
            else -> url
        }
        val limpio = id.trim()
        android.util.Log.d("YOUTUBE_CHECK", "ID Final: $limpio")
        limpio
    } catch (e: Exception) {
        ""
    }
}