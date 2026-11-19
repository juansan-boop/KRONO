package com.krono.core.data.di

import android.content.Context
import androidx.room.Room
import com.krono.core.data.local.room.KronoDatabase
import com.krono.core.data.local.room.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para la capa de persistencia local Room (F-02, D-1).
 */
@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseModule {

    private const val DATABASE_NAME = "krono_database.db"

    /**
     * Provee la instancia única a nivel de aplicación de [KronoDatabase].
     *
     * @param context Contexto global de la aplicación.
     * @return Base de datos [KronoDatabase] configurada con migraciones destructivas por defecto en desarrollo.
     */
    @Provides
    @Singleton
    fun provideKronoDatabase(@ApplicationContext context: Context): KronoDatabase {
        return Room.databaseBuilder(
            context,
            KronoDatabase::class.java,
            DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    /**
     * Provee el DAO [TaskDao] para interactuar con la tabla de tareas cacheadas.
     *
     * @param database Instancia de [KronoDatabase].
     * @return DAO [TaskDao].
     */
    @Provides
    @Singleton
    fun provideTaskDao(database: KronoDatabase): TaskDao {
        return database.taskDao()
    }
}
