package com.example.tfgv01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tfgv01.data.model.Song
import com.example.tfgv01.ui.screens.LibraryScreen
import com.example.tfgv01.ui.screens.PlayerScreen
import com.example.tfgv01.ui.screens.LocalPlayerScreen
import com.example.tfgv01.ui.theme.ArpegioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Activity principal y único punto de entrada de la aplicación Arpeg.io.
 *
 * Implementa navegación manual basada en estado (sin Navigation Component) con
 * cuatro pantallas posibles:
 * - **"splash"**: Pantalla de bienvenida con el logo animado de Arpeg.io.
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
                var currentScreen by remember { mutableStateOf("splash") }
                var selectedSong by remember { mutableStateOf<Song?>(null) }

                // Crossfade suave entre todas las pantallas (300ms)
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(durationMillis = 400),
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "splash" -> SplashScreen(
                            onSplashFinished = { currentScreen = "library" }
                        )

                        "library" -> LibraryScreen(
                            onSongSelected = { song ->
                                selectedSong = song
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
}

/**
 * 🎸 Pantalla de Splash — Logo Arpeg.io animado.
 *
 * Muestra el logo con una animación de escala + fade in,
 * y navega automáticamente a la biblioteca tras 2 segundos.
 */
@Composable
private fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animación de entrada
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBack),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000) // Mostrar splash 2 segundos
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Logo (usa el drawable del adaptive icon)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Arpeg.io",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre de la app
            Text(
                text = "Arpeg.io",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Tu compañero de tablaturas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
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