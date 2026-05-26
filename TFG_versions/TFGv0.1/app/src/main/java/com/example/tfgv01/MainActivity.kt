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
import com.example.tfgv01.ui.screens.LocalPlayerScreen
import com.example.tfgv01.ui.theme.ArpegioTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Activity principal y único punto de entrada de la aplicación Arpeg.io.
 *
 * Implementa navegación manual basada en estado (sin Navigation Component) con
 * tres pantallas posibles:
 * - **"library"**: Pantalla de biblioteca con canciones remotas y locales.
 * - **"player"**: Reproductor con YouTube + partitura sincronizada (canciones de Firestore).
 * - **"local_player"**: Reproductor con metrónomo virtual + partitura local (archivos .gp3).
 *
 * Anotada con @AndroidEntryPoint para habilitar la inyección de dependencias de Hilt
 * en los ViewModels de cada pantalla.
 */
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
                            // El flag isLocal determina qué reproductor usar
                            currentScreen = if (song.isLocal) "local_player" else "player"
                        }
                    )

                    // Flujo Firebase: reproductor con vídeo YouTube sincronizado
                    "player" -> selectedSong?.let { song ->
                        PlayerScreen(
                            song = song,
                            onNavigateBack = { currentScreen = "library" }
                        )
                    }

                    // Flujo local: reproductor con metrónomo virtual y control de tempo
                    "local_player" -> selectedSong?.let { song ->
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

@Preview(showBackground = true)
@Composable
private fun MainActivityPreview() {
    ArpegioTheme {
        LibraryScreen(onSongSelected = {})
    }
}