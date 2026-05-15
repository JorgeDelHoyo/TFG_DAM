// app/src/main/java/com/example/tfgv01/data/repository/SongRepository.kt
package com.example.tfgv01.data.repository

import com.example.tfgv01.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SongRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("canciones")

    fun getSongs(): Flow<List<Song>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSongById(songId: String): Result<Song> = try {
        val document = collection.document(songId).get().await()
        val song = document.toObject(Song::class.java)
        if (song != null) Result.success(song)
        else Result.failure(Exception("Canción no encontrada"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}