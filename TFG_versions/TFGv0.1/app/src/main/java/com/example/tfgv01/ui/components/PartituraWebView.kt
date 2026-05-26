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

/**
 * Componente Compose que renderiza una partitura interactiva dentro de un WebView.
 *
 * Utiliza la librería AlphaTab (cargada en player.html) para convertir archivos de tablatura
 * Guitar Pro (.gp3/.gp4/.gp5/.gpx) en notación musical visual con cursor animado.
 *
 * Soporta dos modos de carga de archivos:
 * - **Remoto (esLocal=false):** Archivos desde /assets/ (empaquetados con la APK o descargados).
 * - **Local (esLocal=true):** Archivos desde filesDir (importados por el usuario).
 *
 * La comunicación Android ↔ JavaScript se realiza mediante [WebView.evaluateJavascript].
 *
 * @param urlArchivo Ruta del archivo de tablatura (nombre de asset o ruta absoluta).
 * @param instrumentIndex Índice del track/instrumento a renderizar en AlphaTab.
 * @param videoDuration Duración total de referencia en segundos (del vídeo YouTube o del score).
 * @param esLocal Si true, el archivo se carga desde el almacenamiento interno del dispositivo.
 * @param modifier Modifier de Compose estándar para layout.
 * @param onWebViewCreated Callback que expone la instancia de WebView al composable padre.
 */
@Composable
fun PartituraWebView(
    urlArchivo: String,
    instrumentIndex: Int,
    videoDuration: Float,
    esLocal: Boolean = false,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val safeInstrumentIndex = instrumentIndex.coerceAtLeast(0)

    // Construimos la URL según el origen del archivo
    val safeUrlArchivo = remember(urlArchivo, esLocal) {
        val trimmed = urlArchivo.trim()
        if (esLocal) {
            // Para archivos locales: mapear la ruta física al path virtual de WebViewAssetLoader
            val nombreArchivo = File(trimmed).name
            "https://appassets.androidplatform.net/local_files/$nombreArchivo"
        } else {
            // Para assets empaquetados: limpiar prefijos redundantes
            trimmed
                .removePrefix("file:///android_asset/")
                .removePrefix("android_asset/")
                .removePrefix("assets/")
                .replace("'", "\\'")
        }
    }

    Log.d("PARTITURA", "Cargando (local=$esLocal): $safeUrlArchivo, track: $safeInstrumentIndex")

    var webView: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { context ->
            // WebViewAssetLoader con doble path handler para resolver peticiones de ambos orígenes
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
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

                // Redirigir logs JS de la consola del WebView a Logcat
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
                    // Interceptar peticiones para que el AssetLoader resuelva rutas virtuales
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                            ?: super.shouldInterceptRequest(view, request)
                    }

                    // Una vez cargado player.html, inicializar AlphaTab con la canción
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("PARTITURA", "player.html cargado. Invocando loadSong('$safeUrlArchivo', $safeInstrumentIndex, $videoDuration)")

                        view?.evaluateJavascript("loadSong('$safeUrlArchivo', $safeInstrumentIndex, $videoDuration)") { result ->
                            Log.d("PARTITURA", "loadSong → $result")
                        }
                    }
                }

                // Cargar player.html a través del protocolo HTTPS virtual (requerido por CORS/AlphaTab)
                loadUrl("https://appassets.androidplatform.net/assets/player.html")

                webView = this
                onWebViewCreated(this)
            }
        },
        modifier = modifier,
        update = { view ->
            // Actualizar el track e instrumento cuando cambian los parámetros de Compose
            view.evaluateJavascript(
                "if (typeof window.changeTrack === 'function') { " +
                        "   changeTrack($safeInstrumentIndex); " +
                        "} else { " +
                        "   console.warn('[Android] changeTrack no definido aún'); " +
                        "}"
            ) { result ->
                Log.d("PARTITURA", "changeTrack → $result")
            }

            view.evaluateJavascript(
                "if (typeof window.setVideoDuration === 'function') { " +
                        "   setVideoDuration($videoDuration); " +
                        "}"
            ) { result ->
                Log.d("PARTITURA", "setVideoDuration → $result")
            }
        }
    )
}

// ============================================================
// Funciones de extensión para control del cursor desde Kotlin
// ============================================================

/** Inicia el avance del cursor desde un segundo dado. */
fun WebView.startAutoScroll(startTime: Float = 0f) {
    this.evaluateJavascript("startAutoScroll($startTime)") { result ->
        Log.d("PARTITURA", "Auto-scroll iniciado: $result")
    }
}

/** Detiene el avance del cursor. */
fun WebView.stopAutoScroll() {
    this.evaluateJavascript("stopAutoScroll()") { result ->
        Log.d("PARTITURA", "Auto-scroll detenido: $result")
    }
}

/** Actualiza la posición del cursor al segundo indicado. */
fun WebView.updateScrollPosition(time: Float) {
    this.evaluateJavascript("updateScrollPosition($time)") { result ->
        Log.d("PARTITURA", "Posición actualizada: $result")
    }
}

/** Corrige la posición del cursor en base al tiempo real del vídeo YouTube. */
fun WebView.correctAutoScrollTime(time: Float) {
    this.evaluateJavascript("correctAutoScrollTime($time)") { result ->
        Log.d("PARTITURA", "Tiempo corregido: $result")
    }
}