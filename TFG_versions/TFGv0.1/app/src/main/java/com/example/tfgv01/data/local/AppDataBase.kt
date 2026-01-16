package com.example.tfgv01.data.local

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfgv01.data.model.Cancion

/**
 * Clase principal de la base de datos Room.
 * Implementa el patrón Singleton para asegurar que solo exista una instancia
 * de la base de datos abierta al mismo tiempo, evitando fugas de memoria.
 */
@Database(entities = [Cancion::class], version = 1)
abstract class AppDatabase : RoomDatabase(){
    companion object {
        /**
         * Obtiene la instancia única de la base de datos.
         * Si no existe, la crea de forma sincronizada.
         */
        fun getDataBase (context : Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).build()
        }
    }
}
