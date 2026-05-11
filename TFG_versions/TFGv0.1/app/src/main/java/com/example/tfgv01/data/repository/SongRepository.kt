package com.example.tfgv01.data.repository

import com.example.tfgv01.data.model.Cancion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SongRepository {
    private val db = FirebaseFirestore.getInstance()
    private val songsCollection = db.collection("canciones")

    // Busca canciones por título (esto es una búsqueda básica de Firestore)
    suspend fun buscarCanciones(query: String): List<Cancion> {
        return try {
            // Firestore no permite búsqueda por texto parcial fácilmente como SQL,
            // pero podemos filtrar por artista o título exacto/prefijo.
            songsCollection
                .whereGreaterThanOrEqualTo("titulo", query)
                .whereLessThanOrEqualTo("titulo", query + "\uf8ff")
                .get()
                .await()
                .toObjects(Cancion::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}