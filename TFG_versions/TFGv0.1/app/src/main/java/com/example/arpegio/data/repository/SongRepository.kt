// app/src/main/java/com/example/tfgv01/data/repository/SongRepository.kt
package com.example.arpegio.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.arpegio.data.local.CancionDao
import com.example.arpegio.data.model.Song
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

/**
 * Repositorio central de canciones. Implementa el patrón Repository para abstraer
 * las dos fuentes de datos de la aplicación:
 *
 * 1. **Firestore (remoto):** Canciones de la comunidad con vídeo de YouTube + tablatura.
 * 2. **Room (local):** Archivos .gp(x) importados por el usuario desde su dispositivo.
 *
 * Inyectado como @Singleton para compartir una única instancia en toda la app.
 *
 * @property firestore Instancia de Firestore inyectada por Hilt.
 * @property cancionDao DAO de Room para operaciones CRUD locales.
 * @property context Contexto de la aplicación para acceder a filesDir.
 */
@Singleton
class SongRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cancionDao: CancionDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SongRepository"
        private const val COLLECTION_NAME = "canciones"
    }

    private val collection = firestore.collection(COLLECTION_NAME)

    /**
     * Obtiene las canciones de la comunidad desde Firestore en tiempo real.
     *
     * Usa callbackFlow + addSnapshotListener para emitir actualizaciones reactivas.
     * Las canciones se ordenan por fecha de creación descendente (más recientes primero).
     *
     * @return Flow que emite la lista actualizada cada vez que Firestore detecta cambios.
     */
    fun getSongs(): Flow<List<Song>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }
                val songs = snapshot?.toObjects(Song::class.java) ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Busca una canción específica por su ID en Firestore.
     *
     * @param songId Identificador del documento en la colección "canciones".
     * @return Result con la canción encontrada o un error descriptivo.
     */
    suspend fun getSongById(songId: String): Result<Song> = try {
        val document = collection.document(songId).get().await()
        val song = document.toObject(Song::class.java)
        if (song != null) Result.success(song)
        else Result.failure(Exception("Canción no encontrada"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Obtiene las canciones locales almacenadas en Room.
     * @return Flow reactivo que se actualiza automáticamente al insertar/eliminar canciones.
     */
    fun getLocalSongs(): Flow<List<Song>> = cancionDao.getLocalSongs()

    /**
     * Importa un archivo local desde el sistema de archivos del dispositivo.
     *
     * Proceso:
     * 1. Copia el archivo desde la URI seleccionada al almacenamiento interno (filesDir).
     * 2. Genera un nombre único para evitar colisiones (user_tab_timestamp.gp3).
     * 3. Inserta los metadatos en Room con isLocal=true.
     *
     * @param title Título asignado por el usuario a la canción.
     * @param fileUri URI del archivo seleccionado por el file picker.
     * @return Result.success si todo OK, Result.failure con el error si falla.
     */
    suspend fun saveLocalSong(title: String, fileUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fileName = "user_tab_${System.currentTimeMillis()}.gp3"
            val targetFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception("No se pudo abrir la partitura seleccionada")

            val localSong = Song(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = "Mis Canciones",
                tabs = mapOf("Guitarra" to targetFile.absolutePath),
                createdAt = Date(),
                isLocal = true
            )

            cancionDao.insertLocalSong(localSong)
            Log.d(TAG, "Canción local guardada: '$title' → ${targetFile.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al importar canción local", e)
            Result.failure(e)
        }
    }

    /** Actualiza los metadatos de una canción local en Room (ej: renombrar título). */
    suspend fun updateLocalSong(song: Song): Unit = withContext(Dispatchers.IO) {
        cancionDao.updateLocalSong(song)
    }

    /**
     * Elimina una canción local: borra el archivo local del almacenamiento
     * interno y elimina el registro de la base de datos Room.
     */
    suspend fun deleteLocalSong(song: Song): Unit = withContext(Dispatchers.IO) {
        val filePath = song.tabs["Guitarra"]
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Archivo eliminado: $filePath")
            }
        }
        cancionDao.deleteLocalSong(song)
    }
}