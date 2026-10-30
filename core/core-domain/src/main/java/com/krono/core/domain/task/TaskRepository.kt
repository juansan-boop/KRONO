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
     * Sin conexión no es un error: se siguen emitiendo los datos de la caché local.
     *
     * @return [Flow] que emite [KronoResult.Success] con la lista ante cada cambio, o un único
     * [KronoResult.Error] (y termina) si la observación falla; para reintentar basta con volver
     * a recolectarlo.
     */
    fun observeTasks(): Flow<KronoResult<List<Task>>>

    /**
     * Observa una tarea puntual por su identificador.
     *
     * @param taskId Identificador único de la tarea.
     * @return [Flow] que emite [KronoResult.Success] con la tarea, o con `null` si no existe o fue
     * eliminada; o un único [KronoResult.Error] (y termina) si la observación falla.
     */
    fun observeTaskById(taskId: String): Flow<KronoResult<Task?>>

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
     * @return [KronoResult.Success] con [Unit] si la escritura se aplicó o quedó encolada sin
     * conexión, o [KronoResult.Error] en caso de fallo.
     */
    suspend fun upsertTask(task: Task): KronoResult<Unit>

    /**
     * Escribe solo los [fields] indicados de [task] sobre la tarea guardada, sin tocar el resto
     * del documento (F-05 CA-2): el tiempo real, las sesiones y cualquier campo no listado se
     * conservan aunque [task] provenga de una lectura vieja.
     *
     * @param task Tarea de la que se toman el identificador y los valores a escribir.
     * @param fields Campos que se escriben.
     * @return [KronoResult.Success] si la escritura se aplicó o quedó encolada sin conexión, o
     * [KronoResult.Error] si falla.
     */
    suspend fun updateTaskFields(task: Task, fields: Set<TaskField>): KronoResult<Unit>

    /**
     * Elimina una tarea y sus subtareas embebidas de Firestore (CA-1).
     *
     * @param taskId Identificador único de la tarea a eliminar.
     * @return [KronoResult.Success] con [Unit] si fue eliminada o el borrado quedó encolado sin
     * conexión, o [KronoResult.Error] en caso de fallo.
     */
    suspend fun deleteTask(taskId: String): KronoResult<Unit>
}
