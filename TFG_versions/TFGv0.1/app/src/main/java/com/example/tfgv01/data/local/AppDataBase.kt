package com.example.tfgv01.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfgv01.data.model.Song

/**
 * Base de datos local de la aplicación, implementada con Room (abstracción sobre SQLite).
 *
 * Contiene una única tabla `canciones_locales` para persistir los archivos .gp3
 * importados por el usuario desde su dispositivo.
 *
 * @see Converters para la serialización de Map, List y Date.
 * @see CancionDao para las operaciones CRUD disponibles.
 */
@Database(entities = [Song::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDataBase : RoomDatabase() {

    /** Proporciona acceso al DAO de canciones locales. */
    abstract fun cancionDao(): CancionDao
}