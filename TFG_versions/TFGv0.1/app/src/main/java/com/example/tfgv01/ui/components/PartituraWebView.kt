package com.example.tfgv01.ui.components

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PartituraWebView(urlArchivo: String, instrumentIndex: Int) {

    Log.d("PARTITURA", "Cargando archivo: $urlArchivo")

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    // Vital para cargar archivos locales desde JS
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Pasamos el nombre del archivo al JavaScript del player.html
                        // Nota: El archivo debe estar en assets/
                        view?.evaluateJavascript("loadSong('$urlArchivo')", null)
                    }
                }

                // ✅ ESTO FALLABA ANTES: Asegúrate de que 'player.html' exista en src/main/assets/
                loadUrl("file:///android_asset/player.html")
            }
        },
        update = { webView ->
            // Si cambias de instrumento, avisamos al JS
            webView.evaluateJavascript("changeTrack($instrumentIndex)", null)
        }
    )
}