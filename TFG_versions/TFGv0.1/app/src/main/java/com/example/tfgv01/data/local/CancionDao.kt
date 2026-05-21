// app/src/main/java/com/example/tfgv01/data/local/CancionDao.kt
package com.example.tfgv01.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tfgv01.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface CancionDao {
    @Query("SELECT * FROM canciones_locales ORDER BY title ASC")
    fun getLocalSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalSong(song: Song)
}