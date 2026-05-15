package com.example.tfgv01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.screens.LibraryScreen
import com.example.tfgv01.ui.screens.PlayerScreen
import com.example.tfgv01.ui.theme.ArpegioTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@AndroidEntryPoint // ✅ Inyección automática de dependencias con Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // ✅ Bordes edge-to-edge (opcional pero moderno)

        // MainActivity.kt
        setContent {
            ArpegioTheme {
                // 🔄 Estado de navegación simple (puedes usar NavHost después)
                var currentScreen by remember { mutableStateOf("library") }
                var selectedSong by remember { mutableStateOf<Song?>(null) }

                when (currentScreen) {
                    "library" -> LibraryScreen(
                        onSongSelected = { song ->
                            selectedSong = song
                            currentScreen = "player"
                        }
                    )
                    "player" -> selectedSong?.let { song ->
                        PlayerScreen(
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