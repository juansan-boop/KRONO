package com.krono.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.krono.core.data.auth.local.AuthDao
import com.krono.core.data.auth.local.SessionEntity
import com.krono.core.data.auth.local.UserEntity

/**
 * Base de datos única de KRONO. Para agregar datos de otra funcionalidad (tareas,
 * sesiones de tiempo...):
 * 1. Agrega la entidad a `entities` y su DAO como función abstracta.
 * 2. Sube `version` y registra una `Migration` en `DatabaseModule` (nunca borrar datos del usuario).
 * 3. Versiona el JSON que Room exporta en `core/core-data/schemas/`.
 */
@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KronoDatabase : RoomDatabase() {

    abstract fun authDao(): AuthDao

    companion object {
        const val NAME = "krono.db"
    }
}
