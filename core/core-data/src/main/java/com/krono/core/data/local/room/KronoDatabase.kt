package com.krono.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos principal de KRONO construida sobre Room (F-02, D-1).
 *
 * Provee persistencia local estructurada mediante SQLite para almacenar en caché
 * offline las entidades de la aplicación y garantizar recuperación estructurada
 * incluso ante reinicios de la aplicación sin conexión de red.
 */
@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class KronoDatabase : RoomDatabase() {

    /**
     * Provee el DAO para operar con la tabla de tareas cacheadas [TaskEntity].
     *
     * @return Instancia de [TaskDao].
     */
    abstract fun taskDao(): TaskDao
}
