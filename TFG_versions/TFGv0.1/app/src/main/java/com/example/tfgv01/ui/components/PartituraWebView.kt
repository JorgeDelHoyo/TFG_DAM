package com.example.tfgv01.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PartituraWebView(urlArchivo: String, instrumentIndex: Int) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    // ESTO ES VITAL: Permite que el HTML lea el archivo de Firebase
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Enviamos la URL de Firebase al JavaScript
                        view?.evaluateJavascript("loadSong('$urlArchivo')", null)
                    }
                }
                loadUrl("file:///android_asset/player.html")
            }
        },
        update = { webView ->
            // Si cambias de instrumento, le avisamos al JS
            webView.evaluateJavascript("changeTrack($instrumentIndex)", null)
        }
    )
}