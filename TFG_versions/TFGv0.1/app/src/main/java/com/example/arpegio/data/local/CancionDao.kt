// app/src/main/java/com/example/tfgv01/data/local/CancionDao.kt
package com.example.arpegio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.arpegio.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para operaciones CRUD sobre la tabla "canciones_locales" en Room.
 *
 * Todas las operaciones de escritura son "suspend" (se ejecutan en una corrutina con Dispatchers.IO).
 * La consulta de lectura devuelve un Flow reactivo que se actualiza automáticamente.
 */
@Dao
interface CancionDao {

    /** Obtiene todas las canciones locales ordenadas alfabéticamente por título. */
    @Query("SELECT * FROM canciones_locales ORDER BY title ASC")
    fun getLocalSongs(): Flow<List<Song>>

    /** Inserta una canción local. Si ya existe un registro con el mismo ID, lo reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalSong(song: Song)

    /** Actualiza los campos de una canción existente (ej: renombrar título). */
    @Update
    suspend fun updateLocalSong(song: Song)

    /** Elimina el registro de una canción de la base de datos Room. */
    @Delete
    suspend fun deleteLocalSong(song: Song)
}