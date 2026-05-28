// app/src/main/java/com/example/tfgv01/data/model/Song.kt
package com.example.arpegio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de datos principal que representa una canción en la aplicación.
 *
 * Esta clase actúa como entidad de Room para la persistencia local Y como
 * modelo de deserialización de Firestore para las canciones remotas de la comunidad.
 *
 * @property id Identificador único. Para canciones remotas viene de Firestore (@DocumentId),
 *              para locales se genera con UUID.
 * @property title Título de la canción (ej: "Killer Queen").
 * @property artist Nombre del artista (ej: "Queen"). Para locales se fija como "Mis Canciones".
 * @property youtubeVideoId ID del vídeo de YouTube para la reproducción sincronizada.
 *                          Vacío para canciones locales que no tienen vídeo asociado.
 * @property tabs Mapa de instrumento → ruta de archivo .gp3.
 *               Para canciones remotas: nombre del archivo en /assets/ (ej: "queen-killer_queen.gp3").
 *               Para canciones locales: ruta absoluta en filesDir (ej: "/data/.../user_tab_12345.gp3").
 * @property difficulty Nivel de dificultad de la pieza: "beginner", "intermediate" o "advanced".
 * @property tags Etiquetas de clasificación (ej: ["rock", "clásico"]).
 * @property createdAt Timestamp de creación. Generado automáticamente por Firestore en canciones remotas.
 * @property isLocal Flag que determina el flujo de reproducción: true = LocalPlayerScreen, false = PlayerScreen.
 */
@Entity(tableName = "canciones_locales")
data class Song(
    @PrimaryKey
    @DocumentId val id: String = "",

    val title: String = "",
    val artist: String = "",
    val youtubeVideoId: String = "",

    val tabs: Map<String, String> = emptyMap(),

    val difficulty: String = "intermediate",
    val tags: List<String> = emptyList(),

    @ServerTimestamp val createdAt: Date? = null,

    val isLocal: Boolean = false
) {
    // Constructor vacío requerido por Firestore para deserialización automática
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

    /** Genera la URL completa de YouTube a partir del videoId. */
    fun getYouTubeUrl(): String = "https://www.youtube.com/watch?v=$youtubeVideoId"

    /** Verifica si la canción tiene tablatura disponible para un instrumento dado. */
    fun hasTabFor(instrument: String): Boolean = tabs.containsKey(instrument)
}