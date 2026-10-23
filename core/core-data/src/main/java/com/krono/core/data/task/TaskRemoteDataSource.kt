package com.krono.core.data.task

import kotlinx.coroutines.flow.Flow

/**
 * Contrato para la fuente remota de datos de tareas en Cloud Firestore (D-3, F-02).
 * Gestiona la colección `users/{userId}/tasks`.
 */
interface TaskRemoteDataSource {

    /**
     * Observa el flujo reactivo de tareas del usuario especificado.
     *
     * @param userId Identificador único del usuario autenticado.
     * @return [Flow] con la lista actualizada de [TaskDto].
     */
    fun observeTasks(userId: String): Flow<List<TaskDto>>

    /**
     * Observa el flujo reactivo de una tarea específica.
     *
     * @param userId Identificador del usuario.
     * @param taskId Identificador de la tarea.
     * @return [Flow] con la [TaskDto] o `null` si no existe o fue eliminada.
     */
    fun observeTask(userId: String, taskId: String): Flow<TaskDto?>

    /**
     * Obtiene una tarea puntual de Firestore.
     *
     * @param userId Identificador del usuario.
     * @param taskId Identificador de la tarea.
     * @return [TaskDto] encontrada o `null`.
     */
    suspend fun getTask(userId: String, taskId: String): TaskDto?

    /**
     * Persiste o actualiza una tarea en Firestore.
     *
     * @param userId Identificador del usuario.
     * @param taskDto DTO de la tarea a guardar.
     */
    suspend fun saveTask(userId: String, taskDto: TaskDto)

    /**
     * Elimina el documento de la tarea de Firestore.
     *
     * @param userId Identificador del usuario.
     * @param taskId Identificador de la tarea a eliminar.
     */
    suspend fun deleteTask(userId: String, taskId: String)
}
