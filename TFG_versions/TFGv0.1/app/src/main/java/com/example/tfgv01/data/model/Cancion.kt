package com.example.tfgv01.data.model

data class Cancion(
    val id: String = "",
    val titulo: String = "",
    val artista: String = "",
    val youtubeVideoId: String = "",
    // Un mapa donde la clave es el instrumento (Guitarra, Bajo, etc.)
    // y el valor es la URL del archivo de la partitura (.gp3)
    val partituras: Map<String, String> = emptyMap()
)