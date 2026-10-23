package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de prueba en memoria para [TaskRepository] utilizado para aislamiento de pruebas unitarias
 * y valores por defecto en casos de uso de perfil.
 */
open class NoOpTaskRepository(
    initialTasks: List<Task> = emptyList(),
) : TaskRepository {

    val tasksFlow = MutableStateFlow(initialTasks)
    var deleteCalls = 0
        private set

    override fun observeTasks(): Flow<List<Task>> = tasksFlow

    override fun observeTaskById(taskId: String): Flow<Task?> =
        MutableStateFlow(tasksFlow.value.find { it.id == taskId })

    override suspend fun getTaskById(taskId: String): KronoResult<Task> {
        val task = tasksFlow.value.find { it.id == taskId }
        return if (task != null) {
            KronoResult.Success(task)
        } else {
            KronoResult.Error(NoSuchElementException("Tarea no encontrada"))
        }
    }

    override suspend fun upsertTask(task: Task): KronoResult<Unit> {
        val updated = tasksFlow.value.toMutableList()
        updated.removeAll { it.id == task.id }
        updated.add(task)
        tasksFlow.value = updated
        return KronoResult.Success(Unit)
    }

    override suspend fun deleteTask(taskId: String): KronoResult<Unit> {
        deleteCalls++
        val updated = tasksFlow.value.toMutableList()
        updated.removeAll { it.id == taskId }
        tasksFlow.value = updated
        return KronoResult.Success(Unit)
    }
}
