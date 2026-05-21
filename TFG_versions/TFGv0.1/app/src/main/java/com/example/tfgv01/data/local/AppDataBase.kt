package com.example.tfgv01.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tfgv01.data.model.Song

@Database(entities = [Song::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // 🔥 ¡ESTA ES LA LÍNEA MÁGICA!
abstract class AppDataBase : RoomDatabase() {

    abstract fun cancionDao(): CancionDao
}