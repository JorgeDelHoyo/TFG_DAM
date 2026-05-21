// app/src/main/java/com/example/tfgv01/data/model/Song.kt
package com.example.tfgv01.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "canciones_locales")
data class Song(
    @PrimaryKey
    @DocumentId val id: String = "",

    val title: String = "",
    val artist: String = "",
    val youtubeVideoId: String = "",

    // Tabs: instrumento -> nombre de archivo .gp3 (En assets/ para remotas, o ruta absoluta para locales)
    val tabs: Map<String, String> = emptyMap(),

    val difficulty: String = "intermediate", // beginner | intermediate | advanced
    val tags: List<String> = emptyList(),

    @ServerTimestamp val createdAt: Date? = null,

    // Flag crítico para saber de dónde proviene el flujo y pintar separadamente en la UI
    val isLocal: Boolean = false
) {
    // 🔥 Constructor vacío requerido por Firestore para deserialización
    constructor() : this(
        id = "",
        title = "",
        artist = "",
        youtubeVideoId = "",
        tabs = emptyMap(),
        difficulty = "intermediate",
        tags = emptyList(),
        createdAt = null,
        isLocal = false
    )

    // 🎵 Helper: obtener la URL completa de YouTube
    fun getYouTubeUrl(): String = "https://www.youtube.com/watch?v=$youtubeVideoId"

    // 🎸 Helper: verificar si tiene tablatura para un instrumento
    fun hasTabFor(instrument: String): Boolean = tabs.containsKey(instrument)
}