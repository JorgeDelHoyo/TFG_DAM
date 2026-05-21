package com.example.tfgv01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.screens.LibraryScreen
import com.example.tfgv01.ui.screens.PlayerScreen
import com.example.tfgv01.ui.screens.LocalPlayerScreen // ✅ IMPORTAMOS LA PANTALLA REAL
import com.example.tfgv01.ui.theme.ArpegioTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArpegioTheme {
                var currentScreen by remember { mutableStateOf("library") }
                var selectedSong by remember { mutableStateOf<Song?>(null) }

                when (currentScreen) {
                    "library" -> LibraryScreen(
                        onSongSelected = { song ->
                            selectedSong = song
                            currentScreen = if (song.isLocal) "local_player" else "player"
                        }
                    )

                    // 🌐 Flujo Firebase (Comunidad)
                    "player" -> selectedSong?.let { song ->
                        PlayerScreen(
                            song = song,
                            onNavigateBack = { currentScreen = "library" }
                        )
                    }

                    // 🆕 Flujo Local (Canciones Propias subidas por el usuario)
                    "local_player" -> selectedSong?.let { song ->
                        // ✅ Ahora llamará a la pantalla del archivo físico con Hilt y WebView
                        LocalPlayerScreen(
                            song = song,
                            onNavigateBack = { currentScreen = "library" }
                        )
                    }
                }
            }
        }
    }
}

// 🎨 Preview básico para desarrollo
@Preview(showBackground = true)
@Composable
private fun MainActivityPreview() {
    ArpegioTheme {
        LibraryScreen(onSongSelected = {})
    }
}