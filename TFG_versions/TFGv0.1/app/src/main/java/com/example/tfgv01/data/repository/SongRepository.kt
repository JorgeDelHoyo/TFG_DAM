package com.example.tfgv01.data.repository

import com.example.tfgv01.data.model.Cancion
import com.example.tfgv01.data.model.PartituraRelacion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SongRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getSongs(): List<Cancion> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("canciones").get().await()

            snapshot.documents.map { doc ->
                // Leemos los campos uno a uno para evitar errores de mayúsculas
                val cancion = Cancion(
                    id = doc.id,
                    titulo = doc.getString("Título") ?: "",
                    artista = doc.getString("Artista") ?: "",
                    link = doc.getString("Link") ?: ""
                )

                // Traemos la subcolección de partituras
                val partsSnapshot = db.collection("canciones")
                    .document(doc.id)
                    .collection("partituras")
                    .get().await()

                cancion.partituras = partsSnapshot.documents.map { partDoc ->
                    PartituraRelacion(
                        instrumentoId = partDoc.getString("instrumentoId") ?: "",
                        archivo = partDoc.getString("archivo") ?: ""
                    )
                }
                cancion
            }
        } catch (e: Exception) {
            android.util.Log.e("FIREBASE", "Error cargando canciones", e)
            emptyList()
        }
    }
}