// app/src/main/java/com/example/tfgv01/ArpegioApp.kt
package com.example.arpegio

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de Arpeg.io.
 *
 * Anotada con @HiltAndroidApp para inicializar el contenedor de dependencias de Hilt.
 * También configura Firestore con persistencia offline habilitada para que la
 * biblioteca de canciones funcione sin conexión a Internet.
 */
@HiltAndroidApp
class ArpegioApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Habilitar caché offline de Firestore para funcionar sin conexión
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
    }
}