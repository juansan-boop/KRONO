package com.krono.core.domain.task

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para observar la lista de tareas del usuario en tiempo real (F-02 CA-2).
 */
class ObserveTasksUseCase(
    private val repository: TaskRepository,
) {
    /** Retorna un [Flow] reactivo con la lista de tareas. */
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
}

/**
 * Caso de uso para observar una tarea específica por su identificador.
 */
class ObserveTaskByIdUseCase(
    private val repository: TaskRepository,
) {
    /** Retorna un [Flow] reactivo con la tarea solicitada o `null` si no existe. */
    operator fun invoke(taskId: String): Flow<Task?> = repository.observeTaskById(taskId)
}

/**
 * Caso de uso para consultar una tarea puntual por su identificador.
 */
class GetTaskByIdUseCase(
    private val repository: TaskRepository,
) {
    /** Consulta la tarea y retorna un [KronoResult]. */
    suspend operator fun invoke(taskId: String): KronoResult<Task> = repository.getTaskById(taskId)
}

/**
 * Caso de uso para crear o actualizar una tarea (F-02 CA-1, CA-3).
 * Valida que el título no esté en blanco antes de invocar la persistencia.
 */
class UpsertTaskUseCase(
    private val repository: TaskRepository,
) {
    /**
     * Persiste la tarea tras validar sus campos obligatorios.
     *
     * @param task Tarea a guardar.
     * @return [KronoResult.Success] en caso de éxito o [KronoResult.Error] si falla.
     */
    suspend operator fun invoke(task: Task): KronoResult<Unit> {
        if (task.title.isBlank()) {
            return KronoResult.Error(TaskError.ValidationError("El título de la tarea no puede estar vacío."))
        }
        return repository.upsertTask(task)
    }
}

/**
 * Caso de uso para eliminar una tarea junto con sus subtareas embebidas (F-02 CA-1, CA-3).
 */
class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {
    /**
     * Elimina la tarea especificada por [taskId].
     *
     * @param taskId Identificador de la tarea a eliminar.
     * @return [KronoResult.Success] en caso de éxito o [KronoResult.Error] si falla.
     */
    suspend operator fun invoke(taskId: String): KronoResult<Unit> {
        if (taskId.isBlank()) {
            return KronoResult.Error(TaskError.ValidationError("El identificador de la tarea no es válido."))
        }
        return repository.deleteTask(taskId)
    }
}
