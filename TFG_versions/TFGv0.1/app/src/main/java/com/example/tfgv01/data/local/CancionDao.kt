package com.example.tfgv01.data.local

import androidx.room.Dao
import androidx.room.Query
import com.example.tfgv01.data.model.Cancion
import kotlinx.coroutines.flow.Flow

@Dao
interface CancionDao{
    /**
     * Devuelve un flujo reactivo (Flow) con todas las canciones.
     * Al usar Flow, la UI se actualizará automáticamente si hay cambios en la DB.
     */
    @Query("SELECT * FROM canciones")
    fun obtenerTodas(): Flow<List<Cancion>>
    // ... (resto de funciones)
}