package com.example.tfgv01.ui.components

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PartituraWebView(
    urlArchivo: String,
    instrumentIndex: Int,
    modifier: Modifier = Modifier
) {
    val safeInstrumentIndex = instrumentIndex.coerceAtLeast(0)
    val safeUrlArchivo = remember(urlArchivo) {
        urlArchivo
            .trim()
            .removePrefix("file:///android_asset/")
            .removePrefix("android_asset/")
            .removePrefix("assets/")
            .replace("'", "\\'")
    }

    Log.d("PARTITURA", "Cargando archivo: $safeUrlArchivo, índice: $safeInstrumentIndex")

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(
                            "PARTITURA_JS",
                            "${consoleMessage?.message()} -- ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}"
                        )
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PARTITURA", "WebView cargada, cargando canción: $safeUrlArchivo")
                        view?.evaluateJavascript("loadSong('$safeUrlArchivo', $safeInstrumentIndex)") { result ->
                            Log.d("PARTITURA", "loadSong ejecutado: $result")
                        }
                    }
                }

                loadUrl("file:///android_asset/player.html")
            }
        },
        modifier = modifier,
        update = { view ->
            Log.d("PARTITURA", "Update WebView - Nuevo índice: $safeInstrumentIndex")

            view.evaluateJavascript(
                "if (typeof window.changeTrack === 'function') { " +
                        "   changeTrack($safeInstrumentIndex); " +
                        "} else { " +
                        "   console.warn('[Android] changeTrack no definido aún'); " +
                        "}"
            ) { result ->
                Log.d("PARTITURA", "changeTrack en update: $result")
            }
        }
    )
}
