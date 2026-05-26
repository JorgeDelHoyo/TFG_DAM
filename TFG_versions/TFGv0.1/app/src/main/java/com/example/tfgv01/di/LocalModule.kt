package com.example.tfgv01.di

import android.content.Context
import androidx.room.Room
import com.example.tfgv01.data.local.AppDataBase
import com.example.tfgv01.data.local.CancionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias de Hilt para la capa de datos local.
 *
 * Proporciona:
 * - La instancia Singleton de [AppDataBase] (Room).
 * - El [CancionDao] extraído de la base de datos.
 *
 * La base de datos se almacena como "arpegio_local_db" en el almacenamiento
 * interno del dispositivo y persiste entre sesiones.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDataBase {
        return Room.databaseBuilder(
            context,
            AppDataBase::class.java,
            "arpegio_local_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCancionDao(database: AppDataBase): CancionDao {
        return database.cancionDao()
    }
}