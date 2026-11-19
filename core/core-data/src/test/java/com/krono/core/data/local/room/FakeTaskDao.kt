package com.krono.core.data.local.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [TaskDao] para verificar las interacciones de persistencia Room
 * sin requerir una instancia de SQLite física en ejecución.
 */
class FakeTaskDao : TaskDao {

    private val tasksMap = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())

    val allTasks: List<TaskEntity> get() = tasksMap.value.values.toList()

    override fun observeTasks(userId: String): Flow<List<TaskEntity>> =
        tasksMap.map { map ->
            map.values.filter { it.userId == userId && !it.isDeleted }
        }.distinctUntilChanged()

    override fun observeTaskById(userId: String, taskId: String): Flow<TaskEntity?> =
        tasksMap.map { map ->
            map[taskId]?.takeIf { it.userId == userId }
        }.distinctUntilChanged()

    override suspend fun getTasks(userId: String): List<TaskEntity> =
        tasksMap.value.values.filter { it.userId == userId && !it.isDeleted }

    override suspend fun getTaskById(userId: String, taskId: String): TaskEntity? =
        tasksMap.value[taskId]?.takeIf { it.userId == userId }

    override suspend fun upsertTask(task: TaskEntity) {
        val current = tasksMap.value.toMutableMap()
        current[task.id] = task
        tasksMap.value = current
    }

    override suspend fun upsertTasks(tasks: List<TaskEntity>) {
        val current = tasksMap.value.toMutableMap()
        tasks.forEach { current[it.id] = it }
        tasksMap.value = current
    }

    override suspend fun getPendingSyncTasks(userId: String): List<TaskEntity> =
        tasksMap.value.values.filter { it.userId == userId && it.syncStatus == TaskEntity.STATUS_PENDING_SYNC }

    override suspend fun clearTasksForUser(userId: String) {
        val current = tasksMap.value.toMutableMap()
        val iterator = current.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.userId == userId) {
                iterator.remove()
            }
        }
        tasksMap.value = current
    }

    override suspend fun deleteTask(userId: String, taskId: String) {
        val current = tasksMap.value.toMutableMap()
        val task = current[taskId]
        if (task?.userId == userId) {
            current.remove(taskId)
            tasksMap.value = current
        }
    }
}
