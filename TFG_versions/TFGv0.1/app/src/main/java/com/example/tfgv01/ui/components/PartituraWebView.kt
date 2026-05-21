// app/src/main/java/com/example/tfgv01/ui/components/PartituraWebView.kt
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
import java.io.File

@Composable
fun PartituraWebView(
    urlArchivo: String,
    instrumentIndex: Int,
    videoDuration: Float,
    esLocal: Boolean = false, // 🆕 Flag crítico para saber el origen de la canción
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val safeInstrumentIndex = instrumentIndex.coerceAtLeast(0)

    // Formateamos la ruta dependiendo de si es un archivo local del almacenamiento o un asset estático
    val safeUrlArchivo = remember(urlArchivo, esLocal) {
        val trimmed = urlArchivo.trim()
        if (esLocal) {
            // Extraemos solo el nombre físico del archivo (ej: user_tab_12345.gp3) de la ruta completa
            val nombreArchivo = File(trimmed).name
            // Le indicamos al JavaScript que acceda mediante el nuevo manejador virtual seguro
            "https://appassets.androidplatform.net/local_files/$nombreArchivo"
        } else {
            // Flujo original intacto para Firebase/Assets
            trimmed
                .removePrefix("file:///android_asset/")
                .removePrefix("android_asset/")
                .removePrefix("assets/")
                .replace("'", "\\'")
        }
    }

    Log.d("PARTITURA", "Cargando archivo (Modo Local=$esLocal): $safeUrlArchivo, índice: $safeInstrumentIndex")

    var webView: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { context ->
            // 🆕 Creamos un AssetLoader con DOBLE manejador de rutas: Assets + Internal Storage
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                // Manejador 1: Para player.html y partituras de la comunidad (Firebase)
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                // Manejador 2: Para abrir partituras .gp3 dinámicas subidas por el usuario en filesDir
                .addPathHandler("/local_files/", WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir))
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

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        // El interceptor ahora resolverá tanto las peticiones de /assets/ como las de /local_files/
                        return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                            ?: super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PARTITURA", "WebView cargada, cargando canción: $safeUrlArchivo con duración: $videoDuration")

                        // Enviamos la URL construida al motor JS de player.html
                        view?.evaluateJavascript("loadSong('$safeUrlArchivo', $safeInstrumentIndex, $videoDuration)") { result ->
                            Log.d("PARTITURA", "loadSong ejecutado: $result")
                        }
                    }
                }

                // Cargar player.html vía HTTPS (Mantiene tu misma infraestructura)
                loadUrl("https://appassets.androidplatform.net/assets/player.html")

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

// 🔥 Tus funciones de extensión para controlar el scroll se quedan 100% intactas
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