package com.example.tfgv01.ui.components

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

@Composable
fun PartituraWebView(
    urlArchivo: String,
    instrumentIndex: Int,
    videoDuration: Float,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
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

    var webView: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { context ->
            // ✅ AssetLoader creado aquí, usando el 'context' del factory (NO LocalContext.current)
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    mediaPlaybackRequiresUserGesture = false
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

                // ✅ WebViewClient con interceptor para WebViewAssetLoader
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                            ?: super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PARTITURA", "WebView cargada, cargando canción: $safeUrlArchivo con duración: $videoDuration")
                        view?.evaluateJavascript("loadSong('$safeUrlArchivo', $safeInstrumentIndex, $videoDuration)") { result ->
                            Log.d("PARTITURA", "loadSong ejecutado: $result")
                        }
                    }
                }

                // ✅ Cargar player.html vía HTTPS (no file://)
                loadUrl("https://appassets.androidplatform.net/assets/player.html")

                // Guardar la referencia al WebView
                webView = this
                onWebViewCreated(this)
            }
        },
        modifier = modifier,
        update = { view ->
            Log.d("PARTITURA", "Update WebView - Nuevo índice: $safeInstrumentIndex, Duración: $videoDuration")

            view.evaluateJavascript(
                "if (typeof window.changeTrack === 'function') { " +
                        "   changeTrack($safeInstrumentIndex); " +
                        "} else { " +
                        "   console.warn('[Android] changeTrack no definido aún'); " +
                        "}"
            ) { result ->
                Log.d("PARTITURA", "changeTrack en update: $result")
            }

            view.evaluateJavascript(
                "if (typeof window.setVideoDuration === 'function') { " +
                        "   setVideoDuration($videoDuration); " +
                        "}"
            ) { result ->
                Log.d("PARTITURA", "setVideoDuration en update: $result")
            }
        }
    )
}

// Funciones de extensión para controlar el scroll
fun WebView.startAutoScroll(startTime: Float = 0f) {
    this.evaluateJavascript("startAutoScroll($startTime)") { result ->
        Log.d("PARTITURA", "Auto-scroll iniciado: $result")
    }
}

fun WebView.stopAutoScroll() {
    this.evaluateJavascript("stopAutoScroll()") { result ->
        Log.d("PARTITURA", "Auto-scroll detenido: $result")
    }
}

fun WebView.updateScrollPosition(time: Float) {
    this.evaluateJavascript("updateScrollPosition($time)") { result ->
        Log.d("PARTITURA", "Posición de scroll actualizada: $result")
    }
}

fun WebView.correctAutoScrollTime(time: Float) {
    this.evaluateJavascript("correctAutoScrollTime($time)") { result ->
        Log.d("PARTITURA", "Tiempo de auto-scroll corregido: $result")
    }
}
