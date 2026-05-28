package com.example.arpegio.di

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias de Hilt para los servicios de Firebase.
 *
 * Proporciona la instancia Singleton de [FirebaseFirestore] que será inyectada
 * en el [com.example.arpegio.data.repository.SongRepository] para acceder a la
 * colección "canciones" de la comunidad.
 *
 * Instalado en [SingletonComponent] para que la instancia persista durante
 * todo el ciclo de vida de la aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}