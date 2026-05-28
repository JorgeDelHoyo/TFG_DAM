package com.example.arpegio.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

data class SeekEvent(
    val seconds: Float,
    val timestamp: Long = System.nanoTime()
)

data class ExternalControls(
    val play: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false, // 👈 Agregamos el estado de mute
    val seekEvent: SeekEvent? = null // 👈 Evento único de seek con timestamp para evitar repeticiones
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(
    videoId: String,
    autoplay: Boolean = false,
    onCurrentSecond: (Float) -> Unit = {},
    onDurationReady: (Float) -> Unit = {},
    externalControls: ExternalControls? = null,
    modifier: Modifier = Modifier
) {
    val normalizedVideoId = remember(videoId) { videoId.extractYouTubeVideoId() }
    val latestOnCurrentSecond by rememberUpdatedState(onCurrentSecond)
    val latestOnDurationReady by rememberUpdatedState(onDurationReady)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastProcessedSeekEventTimestamp by remember { mutableStateOf<Long?>(null) } // 👈 Recordar el timestamp del último seek procesado

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.loadsImagesAutomatically = true
                settings.databaseEnabled = true
                settings.userAgentString = "${settings.userAgentString} Arpegio/1.0"
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(android.graphics.Color.BLACK)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(
                    YouTubeBridge(
                        this,
                        onSecondChanged = { seconds -> latestOnCurrentSecond(seconds) },
                        onDurationChanged = { duration -> latestOnDurationReady(duration) }
                    ),
                    "AndroidPlayer"
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(
                            "YOUTUBE_WEBVIEW",
                            "${consoleMessage?.message()} -- ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}"
                        )
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (request?.url?.lastPathSegment == "favicon.ico") {
                            return WebResourceResponse(
                                "image/x-icon",
                                null,
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e(
                            "YOUTUBE_WEBVIEW",
                            "Error WebView ${error?.errorCode}: ${error?.description} en ${request?.url}"
                        )
                    }
                }

                loadYouTubeHtml(normalizedVideoId, autoplay)
            }
        },
        modifier = modifier,
        update = { webView ->
            if (webView.url == null) {
                webView.loadYouTubeHtml(normalizedVideoId, autoplay)
            }

            externalControls?.let { controls ->
                val command = if (controls.play) "playVideo" else "pauseVideo"
                webView.evaluateJavascript("if (window.$command) { $command(); }", null)
                webView.evaluateJavascript(
                    "if (window.setPlaybackSpeed) { setPlaybackSpeed(${controls.playbackSpeed.toValidYouTubeRate()}); }",
                    null
                )
                // 👈 Evaluamos dinámicamente si hay que mutear o desmutear en el JS
                val muteCommand = if (controls.isMuted) "muteVideo" else "unmuteVideo"
                webView.evaluateJavascript("if (window.$muteCommand) { $muteCommand(); }", null)

                // 👈 Si hay un evento de seek y no ha sido procesado aún
                controls.seekEvent?.let { event ->
                    if (event.timestamp != lastProcessedSeekEventTimestamp) {
                        lastProcessedSeekEventTimestamp = event.timestamp
                        webView.evaluateJavascript("if (window.seekTo) { seekTo(${event.seconds}); }", null)
                    }
                }
            }
        }
    )
}

private class YouTubeBridge(
    private val webView: WebView,
    private val onSecondChanged: (Float) -> Unit,
    private val onDurationChanged: (Float) -> Unit
) {
    @JavascriptInterface
    fun onCurrentSecond(seconds: Float) {
        webView.post {
            onSecondChanged(seconds)
        }
    }

    @JavascriptInterface
    fun onDurationReady(duration: Float) {
        webView.post {
            onDurationChanged(duration)
        }
    }
}

private fun WebView.loadYouTubeHtml(videoId: String, autoplay: Boolean) {
    Log.d("YOUTUBE_WEBVIEW", "Cargando video '$videoId'")
    loadDataWithBaseURL(
        "$YOUTUBE_EMBED_ORIGIN/",
        buildYouTubeHtml(videoId, autoplay),
        "text/html",
        "UTF-8",
        null
    )
}

private fun buildYouTubeHtml(videoId: String, autoplay: Boolean): String {
    val autoPlayValue = if (autoplay) 1 else 0
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                html, body, #player {
                    width: 100%;
                    height: 100%;
                    min-width: 200px;
                    min-height: 200px;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    background: #000;
                    display: block;
                }
            </style>
        </head>
        <body>
            <iframe
                id="player"
                src="https://www.youtube.com/embed/$videoId?enablejsapi=1&origin=$YOUTUBE_EMBED_ORIGIN&playsinline=1&rel=0&controls=1&autoplay=$autoPlayValue"
                title="YouTube video player"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                referrerpolicy="strict-origin-when-cross-origin"
                allowfullscreen>
            </iframe>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                let player = null;
                let pendingPlay = false;
                let pendingRate = 1;
                let pendingMute = false; // Guardado por si la API no está lista

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        events: {
                            onReady: onPlayerReady,
                            onError: onPlayerError,
                            onStateChange: onPlayerStateChange
                        }
                    });
                }

                function onPlayerReady() {
                    setPlaybackSpeed(pendingRate);
                    if (pendingPlay) { player.playVideo(); }
                    if (pendingMute) { player.mute(); } else { player.unMute(); }
                    setInterval(reportCurrentSecond, 500);
                    console.log('[YT] player ready');
                }

                function onPlayerError(event) {
                    console.error('[YT] player error: ' + event.data);
                }

                function onPlayerStateChange(event) {
                    console.log('[YT] player state: ' + event.data);
                }

                function playVideo() {
                    pendingPlay = true;
                    if (player && player.playVideo) { player.playVideo(); }
                }

                function pauseVideo() {
                    pendingPlay = false;
                    if (player && player.pauseVideo) { player.pauseVideo(); }
                }

                function setPlaybackSpeed(rate) {
                    pendingRate = rate;
                    if (player && player.setPlaybackRate) { player.setPlaybackRate(rate); }
                }

                // 👈 Funciones JS mapeadas para la API de YouTube
                function muteVideo() {
                    pendingMute = true;
                    if (player && player.mute) { player.mute(); }
                }

                function unmuteVideo() {
                    pendingMute = false;
                    if (player && player.unMute) { player.unMute(); }
                }

                function seekTo(seconds) {
                    if (player && player.seekTo) {
                        player.seekTo(seconds, true);
                    }
                }

                 let durationReported = false;
                 function reportCurrentSecond() {
                     if (player && player.getCurrentTime && window.AndroidPlayer) {
                         window.AndroidPlayer.onCurrentSecond(player.getCurrentTime());
                         if (!durationReported && player.getDuration) {
                             let d = player.getDuration();
                             if (d > 0) {
                                 if (window.AndroidPlayer.onDurationReady) {
                                     window.AndroidPlayer.onDurationReady(d);
                                     durationReported = true;
                                 }
                             }
                         }
                     }
                 }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private const val YOUTUBE_EMBED_ORIGIN = "https://appassets.androidplatform.net"

private fun Float.toValidYouTubeRate(): Float = when {
    this <= 0.25f -> 0.25f
    this <= 0.5f -> 0.5f
    this <= 1.0f -> 1.0f
    this <= 1.5f -> 1.5f
    else -> 2.0f
}

private fun String.extractYouTubeVideoId(): String {
    val value = trim()
    if (value.length == 11 && !value.contains("/") && !value.contains("?")) return value

    return runCatching {
        val uri = Uri.parse(value)
        uri.getQueryParameter("v")
            ?: uri.lastPathSegment?.takeIf { uri.host.orEmpty().contains("youtu.be") }
            ?: uri.pathSegments.nextSegmentAfter("embed")
            ?: uri.pathSegments.nextSegmentAfter("shorts")
    }.getOrNull()
        ?.take(11)
        ?: value
}

private fun List<String>.nextSegmentAfter(segment: String): String? {
    val index = indexOf(segment)
    return if (index >= 0) getOrNull(index + 1) else null
}