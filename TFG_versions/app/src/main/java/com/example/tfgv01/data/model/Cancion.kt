package com.example.tfgv01.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Cancion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,         // Titulo de la cancion
    val artista: String,        // Artista de la cancion
    val bpm: Int,               // Tempo de la cancion
    val youtubeId: String,      //ID del video
    val archivoXmlPath: String, // Ruta en assets (ejemplo.xml)
    val offset: Float = 0f      // Ajust de tiempo
)