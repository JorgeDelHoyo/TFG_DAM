package com.example.tfgv01.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tfgv01.ui.components.PartituraWebView
import com.example.tfgv01.ui.components.YoutubePlayer
import com.example.tfgv01.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val cancion = viewModel.selectedSong.value

    if (cancion == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("No se ha seleccionado ninguna canción", modifier = Modifier.padding(16.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Parte superior: YouTube (ocupa el 30% de la pantalla aprox)
            Box(modifier = Modifier.height(250.dp)) {
                YoutubePlayer(videoId = cancion.youtubeVideoId)
            }

            Divider()

            // Parte inferior: Partitura (AlphaTab)
            Box(modifier = Modifier.fillWeight(1f)) {
                // De momento usamos el archivo local que tienes en assets
                PartituraWebView(fileName = "queen-killer_queen.gp3", instrumentIndex = 0)
            }
        }
    }
}

// Extensión rápida para manejar pesos en Column
@Composable
fun Modifier.fillWeight(weight: Float): Modifier = this.then(Modifier.fillMaxHeight().fillMaxWidth())