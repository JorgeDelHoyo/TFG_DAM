package com.example.tfgv01.data.model

import androidx.room.Entity

/**
 * Representa la entidad "Canción" en la base de datos local.
 *
 * @property id Identificador único autogenerado.
 * @property titulo Título de la canción.
 * @property artista Nombre del grupo o compositor.
 * @property bpm Beats por minuto (velocidad de la canción).
 * @property youtubeId ID del video de YouTube (ej: "dQw4w9WgXcQ").
 * @property archivoXmlPath Nombre del archivo MusicXML en la carpeta assets.
 * @property offset Ajuste temporal en segundos para sincronizar audio y partitura.
 */

@Entity(tableName = "canciones")
class Cancion (

)