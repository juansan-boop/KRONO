package com.krono.core.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión y consulta SQL de tareas cacheadas localmente en Room (F-02).
 */
@Dao
interface TaskDao {

    /**
     * Observa el flujo reactivo de tareas activas (no eliminadas) de un usuario en particular.
     *
     * @param userId Identificador del usuario.
     * @return [Flow] que emite la lista de [TaskEntity] ante cualquier modificación en la base de datos.
     */
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId AND isDeleted = 0")
    fun observeTasks(userId: String): Flow<List<TaskEntity>>

    /**
     * Observa el flujo reactivo de una tarea específica por su identificador.
     *
     * @param userId Identificador del usuario propietario.
     * @param taskId Identificador único de la tarea.
     * @return [Flow] con la entidad encontrada o `null` si no existe.
     */
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId AND id = :taskId LIMIT 1")
    fun observeTaskById(userId: String, taskId: String): Flow<TaskEntity?>

    /**
     * Consulta síncrona/suspendida de las tareas activas para un usuario.
     *
     * @param userId Identificador del usuario.
     * @return Lista inmutable de [TaskEntity].
     */
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId AND isDeleted = 0")
    suspend fun getTasks(userId: String): List<TaskEntity>

    /**
     * Consulta una tarea específica por identificador y usuario.
     *
     * @param userId Identificador del usuario.
     * @param taskId Identificador de la tarea.
     * @return [TaskEntity] encontrada o `null` si no existe.
     */
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId AND id = :taskId LIMIT 1")
    suspend fun getTaskById(userId: String, taskId: String): TaskEntity?

    /**
     * Inserta o actualiza una tarea en la base de datos local.
     *
     * @param task Entidad a persistir.
     */
    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    /**
     * Inserta o actualiza una lista de tareas en bloque en la base de datos local.
     *
     * @param tasks Lista de entidades a persistir.
     */
    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    /**
     * Obtiene las tareas pendientes de sincronización con el servidor remoto para un usuario.
     *
     * @param userId Identificador del usuario.
     * @return Lista de entidades en estado [TaskEntity.STATUS_PENDING_SYNC].
     */
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncTasks(userId: String): List<TaskEntity>

    /**
     * Elimina todas las tareas almacenadas en caché para un usuario específico.
     *
     * @param userId Identificador del usuario cuyas tareas serán purgadas.
     */
    @Query("DELETE FROM tasks_cache WHERE userId = :userId")
    suspend fun clearTasksForUser(userId: String)

    /**
     * Elimina físicamente una tarea concreta de la caché local.
     *
     * @param userId Identificador del usuario propietario.
     * @param taskId Identificador de la tarea a eliminar.
     */
    @Query("DELETE FROM tasks_cache WHERE userId = :userId AND id = :taskId")
    suspend fun deleteTask(userId: String, taskId: String)
}
