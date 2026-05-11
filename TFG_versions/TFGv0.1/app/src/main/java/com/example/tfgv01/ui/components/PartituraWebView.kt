package com.example.tfgv01.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PartituraWebView(fileName: String, instrumentIndex: Int) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Cuando carga el HTML, llamamos a la función JS que definiste
                    // fileName será la URL de Firebase o el archivo local
                    view?.evaluateJavascript("loadSong('$fileName')", null)
                }
            }
            loadUrl("file:///android_asset/player.html")
        }
    }, update = { webView ->
        // Si el instrumento cambia, notificamos al JS
        webView.evaluateJavascript("changeTrack($instrumentIndex)", null)
    })
}