package com.example.tfgv01.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

data class ExternalControls(
    val play: Boolean = false,
    val playbackSpeed: Float = 1.0f
)

@Composable
fun YouTubePlayer(
    videoId: String,
    autoplay: Boolean = false,
    onPlayerReady: (YouTubePlayer) -> Unit = {},
    externalControls: ExternalControls? = null,
    modifier: Modifier = Modifier
) {
    var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }

    AndroidView(
        factory = { context ->
            YouTubePlayerView(context).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayerRef = youTubePlayer
                        youTubePlayer.loadVideo(videoId, 0f)
                        if (!autoplay) youTubePlayer.pause()
                        onPlayerReady(youTubePlayer)
                    }
                })
            }
        },
        modifier = modifier,
        update = { view ->
            externalControls?.let { controls ->
                youTubePlayerRef?.let { player ->
                    if (controls.play) player.play() else player.pause()
                    // ✅ La biblioteca acepta Float directamente (NO usa enum PlaybackRate)
                    player.setPlaybackRate(controls.playbackSpeed)
                }
            }
        }
    )
}