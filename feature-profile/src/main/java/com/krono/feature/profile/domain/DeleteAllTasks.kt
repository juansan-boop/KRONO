package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Borra todas las tareas del usuario (`users/{uid}/tasks`) comprobando el resultado de cada borrado.
 *
 * Se detiene en el primer fallo y lo propaga, para que quien llama no continúe con pasos
 * destructivos ni informe un éxito falso.
 *
 * @return [KronoResult.Success] si no quedó ninguna tarea por borrar, o el primer [KronoResult.Error].
 */
internal suspend fun TaskRepository.deleteAllTasks(): KronoResult<Unit> {
    val tasks = try {
        observeTasks().first()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        return KronoResult.Error(error)
    }
    for (task in tasks) {
        val result = deleteTask(task.id)
        if (result is KronoResult.Error) return result
    }
    return KronoResult.Success(Unit)
}
