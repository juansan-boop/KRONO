package com.krono.core.data.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [TaskRemoteDataSource] para pruebas unitarias sin el SDK de Firestore.
 */
class FakeTaskRemoteDataSource : TaskRemoteDataSource {

    /** Almacén en memoria indexado por userId -> (taskId -> TaskDto). */
    private val data = MutableStateFlow<Map<String, Map<String, TaskDto>>>(emptyMap())

    /** Error forzado opcional para simular caídas del SDK o red. */
    var failure: Exception? = null

    /** Inserta o actualiza una tarea en memoria para un usuario dado. */
    fun givenTask(userId: String, taskDto: TaskDto) {
        val current = data.value.toMutableMap()
        val userTasks = current[userId]?.toMutableMap() ?: mutableMapOf()
        userTasks[taskDto.id] = taskDto
        current[userId] = userTasks
        data.value = current
    }

    override fun observeTasks(userId: String): Flow<List<TaskDto>> {
        failure?.let { throw it }
        return data.map { map ->
            map[userId]?.values?.toList().orEmpty()
        }
    }

    override fun observeTask(userId: String, taskId: String): Flow<TaskDto?> {
        failure?.let { throw it }
        return data.map { map ->
            map[userId]?.get(taskId)
        }
    }

    override suspend fun getTask(userId: String, taskId: String): TaskDto? {
        failure?.let { throw it }
        return data.value[userId]?.get(taskId)
    }

    override suspend fun saveTask(userId: String, taskDto: TaskDto) {
        failure?.let { throw it }
        givenTask(userId, taskDto)
    }

    override suspend fun deleteTask(userId: String, taskId: String) {
        failure?.let { throw it }
        val current = data.value.toMutableMap()
        val userTasks = current[userId]?.toMutableMap() ?: return
        userTasks.remove(taskId)
        current[userId] = userTasks
        data.value = current
    }
}
