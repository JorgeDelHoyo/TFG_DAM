package com.example.tfgv01// <--- Tu paquete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tfgv01.ui.screens.PlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Llamamos a la pantalla que vamos a crear ahora
            PlayerScreen()
        }
    }
}