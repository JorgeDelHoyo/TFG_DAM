package com.example.tfgv01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.tfgv01.ui.screens.LibraryScreen
import com.example.tfgv01.ui.screens.PlayerScreen
import com.example.tfgv01.ui.viewmodel.PlayerViewModel
import com.example.tfgv01.ui.theme.TFGv01Theme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    // Instanciamos el ViewModel aquí para que sobreviva a cambios de pantalla
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TFGv01Theme {
                // Usamos 'by' para que Compose sepa que debe observar cambios
                // Asegúrate de importar: import androidx.compose.runtime.getValue
                val selectedSong by playerViewModel.selectedSong

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (selectedSong == null) {
                        LibraryScreen(
                            viewModel = playerViewModel,
                            onSongSelected = { /* Compose se encargará de recomponer al detectar el cambio en selectedSong */ }
                        )
                    } else {
                        PlayerScreen(viewModel = playerViewModel)
                    }
                }
            }
        }
    }

    // Para que al pulsar "atrás" vuelva a la lista en lugar de cerrar la app
    override fun onBackPressed() {
        if (playerViewModel.selectedSong.value != null) {
            playerViewModel.selectSong(null) // Reset para volver a la librería
        } else {
            super.onBackPressed()
        }
    }
}