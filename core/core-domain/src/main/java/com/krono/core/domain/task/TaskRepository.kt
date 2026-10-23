package com.krono.core.domain.task

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para la gestión y persistencia reactiva de tareas (F-02).
 */
interface TaskRepository {

    /**
     * Observa la lista completa de tareas del usuario autenticado en tiempo real.
     *
     * @return [Flow] que emite la lista ordenada de tareas ante cada cambio en la base de datos.
     */
    fun observeTasks(): Flow<List<Task>>

    /**
     * Observa una tarea puntual por su identificador.
     *
     * @param taskId Identificador único de la tarea.
     * @return [Flow] que emite la tarea correspondiente o `null` si no existe o fue eliminada.
     */
    fun observeTaskById(taskId: String): Flow<Task?>

    /**
     * Obtiene una tarea puntual por su identificador de manera suspendida.
     *
     * @param taskId Identificador único de la tarea.
     * @return [KronoResult.Success] con la tarea encontrada, o [KronoResult.Error] si falla o no existe.
     */
    suspend fun getTaskById(taskId: String): KronoResult<Task>

    /**
     * Crea o actualiza una tarea completa (incluyendo sus subtareas embebidas) en Firestore.
     *
     * @param task Tarea con sus datos y lista de subtareas a persistir.
     * @return [KronoResult.Success] con [Unit] si la operación fue exitosa, o [KronoResult.Error] en caso de fallo.
     */
    suspend fun upsertTask(task: Task): KronoResult<Unit>

    /**
     * Elimina una tarea y sus subtareas embebidas de Firestore (CA-1).
     *
     * @param taskId Identificador único de la tarea a eliminar.
     * @return [KronoResult.Success] con [Unit] si fue eliminada, o [KronoResult.Error] en caso de fallo.
     */
    suspend fun deleteTask(taskId: String): KronoResult<Unit>
}
