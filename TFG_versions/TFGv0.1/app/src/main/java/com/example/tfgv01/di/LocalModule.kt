package com.example.tfgv01.di // O el paquete donde tengas tus módulos

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
            "arpegio_local_db" // Nombre del archivo de la base de datos en el teléfono
        )
            // .fallbackToDestructiveMigration() // Opcional: Borra la BD si cambias la estructura en desarrollo sin hacer migraciones
            .build()
    }

    @Provides
    @Singleton
    fun provideCancionDao(database: AppDataBase): CancionDao {
        // Le enseñamos a Hilt que para conseguir el DAO, primero agarre la base de datos y llame al método abstracto
        return database.cancionDao()
    }
}