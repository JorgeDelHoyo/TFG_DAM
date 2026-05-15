// app/src/main/java/com/example/tfgv01/ArpegioApp.kt
package com.example.tfgv01

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ArpegioApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 🔥 Configuración opcional de Firestore
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // ✅ Caché offline
            .build()
        firestore.firestoreSettings = settings
    }
}