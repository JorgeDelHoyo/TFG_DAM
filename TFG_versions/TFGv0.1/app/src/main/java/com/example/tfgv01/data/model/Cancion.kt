package com.example.tfgv01.data.model

import com.google.firebase.firestore.PropertyName

data class Cancion(
    var id: String = "",
    var titulo: String = "",
    var artista: String = "",
    var link: String = "",
    var partituras: List<PartituraRelacion> = emptyList()
)

data class PartituraRelacion(
    var instrumentoId: String = "",
    var archivo: String = ""
)