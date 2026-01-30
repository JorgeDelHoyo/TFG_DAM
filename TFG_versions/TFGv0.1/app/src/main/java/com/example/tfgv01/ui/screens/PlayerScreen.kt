package com.example.tfgv01.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PlayerScreen() {
    Column(modifier = Modifier.fillMaxSize()) {

        // 1. EL REPRODUCTOR "TRUCADO" DE YOUTUBE
        YoutubeWebHack(
            videoId = "bR-gZQLO26w", // El vídeo que me has pasado
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        // 2. LA PARTITURA (ALPHATAB)
        PartituraWebViewEmbed(
            archivoXml = "queen-killer_queen.gp3",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

// 👇👇 AQUÍ ESTÁ EL TRUCO PARA QUE YOUTUBE NO TE BLOQUEE 👇👇
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebHack(videoId: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                // HTML QUE INCRUSTA EL VÍDEO A LA FUERZA
                val videoHtml = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:0;">
                        <iframe 
                            width="100%" 
                            height="100%" 
                            src="https://www.youtube.com/embed/$videoId?playsinline=1" 
                            frameborder="0" 
                            allow="autoplay; encrypted-media" 
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()

                // EL SECRETO: Le decimos a YouTube que venimos de "https://www.youtube.com"
                // Esto suele saltarse el bloqueo de aplicaciones desconocidas.
                loadDataWithBaseURL(
                    "https://www.songsterr.com",  // <--- CAMBIA ESTO (Antes ponía youtube.com)
                    videoHtml,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PartituraWebViewEmbed(archivoXml: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        evaluateJavascript("loadSong('$archivoXml')", null)
                    }
                }
                loadUrl("file:///android_asset/player.html")
            }
        }
    )
}