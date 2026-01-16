package com.example.tfgv01.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Wrapper de Jetpack Compose para la librería "Android-YouTube-Player".
 *
 * Gestiona el ciclo de vida del reproductor y carga el video especificado.
 *
 * @param videoId El identificador del video de YouTube a reproducir.
 */
@Composable
fun YouTubePlayer(
    videoId:String,
    modifier: Modifier = Modifier
){
    AndroidView(
        modifier = modifier,
        factory = {context ->
            YouTubePlayerView(context).apply {
                addYouTubePlayerListener(youTubePlayerListener =  object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, startSeconds = 0f)
                    }
                })
            }
        }
    )
}