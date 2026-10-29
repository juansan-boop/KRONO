package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.Task
import com.krono.feature.profile.data.NoOpTaskRepository

/**
 * Repositorio de tareas de prueba que registra cada borrado en [log] (el mismo registro del
 * origen remoto) y permite simular que una tarea concreta falla al borrarse.
 */
internal class RecordingTaskRepository(
    initialTasks: List<Task>,
    private val log: MutableList<String>,
    private val failingTaskId: String? = null,
) : NoOpTaskRepository(initialTasks) {

    override suspend fun deleteTask(taskId: String): KronoResult<Unit> {
        log += "deleteTask:$taskId"
        if (taskId == failingTaskId) return KronoResult.Error(IllegalStateException("fallo al borrar $taskId"))
        return super.deleteTask(taskId)
    }
}
