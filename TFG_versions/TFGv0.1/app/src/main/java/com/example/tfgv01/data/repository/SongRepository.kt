// app/src/main/java/com/example/tfgv01/data/repository/SongRepository.kt
package com.example.tfgv01.data.repository

import android.content.Context
import android.net.Uri
import com.example.tfgv01.data.local.CancionDao
import com.example.tfgv01.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cancionDao: CancionDao,
    @ApplicationContext private val context: Context
) {
    private val collection = firestore.collection("canciones")

    // 🌐 Obtener canciones desde Firestore (Comunidad)
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

    // 💾 LOCAL: Obtener canciones de Room
    fun getLocalSongs(): Flow<List<Song>> = cancionDao.getLocalSongs()

    // 💾 LOCAL: Guardar archivo .gp3 físico e insertar metadatos en Room
    suspend fun saveLocalSong(title: String, fileUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Nombre físico único para evitar sobreescribir canciones con igual título
            val fileName = "user_tab_${System.currentTimeMillis()}.gp3"
            val targetFile = File(context.filesDir, fileName)

            // Copiamos la corriente binaria de bytes del archivo seleccionado al almacenamiento local
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception("No se pudo abrir la partitura seleccionada")

            // Generamos la nueva entidad reutilizando tu Song data class
            val localSong = Song(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = "Mis Canciones",
                tabs = mapOf("Guitarra" to targetFile.absolutePath), // Guardamos la ruta absoluta del almacenamiento interno
                createdAt = Date(),
                isLocal = true
            )

            cancionDao.insertLocalSong(localSong)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}