package com.krono.core.data.task

import com.krono.core.domain.task.TaskError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [TaskRemoteDataSource] para pruebas unitarias sin el SDK de Firestore.
 *
 * Imita la caché local de Firestore: las escrituras se aplican en memoria al instante y, si hay
 * [serverConfirmation], después esperan a que se complete, como la confirmación del servidor.
 */
class FakeTaskRemoteDataSource : TaskRemoteDataSource {

    /** Almacén en memoria indexado por userId -> (taskId -> TaskDto). */
    private val data = MutableStateFlow<Map<String, Map<String, TaskDto>>>(emptyMap())

    /** Error forzado opcional para simular caídas del SDK o red. */
    var failure: Exception? = null

    /** Error con el que fallan los listeners nuevos, como un `PERMISSION_DENIED` de Firestore. */
    var listenerFailure: Exception? = null

    /**
     * Confirmación del servidor que esperan las escrituras tras aplicarse localmente; `null` para
     * confirmar al instante. Si nunca se completa, simula estar sin conexión.
     */
    var serverConfirmation: CompletableDeferred<Unit>? = null

    /** Campos enviados en la última escritura parcial. */
    var lastUpdatedFields: Map<String, Any?> = emptyMap()
        private set

    /** Cantidad de listeners de la lista abiertos, para probar reintentos. */
    var tasksListenerCount: Int = 0
        private set

    /** Cantidad de listeners de una tarea puntual abiertos, para probar reintentos. */
    var taskListenerCount: Int = 0
        private set

    /** Inserta o actualiza una tarea en memoria para un usuario dado. */
    fun givenTask(userId: String, taskDto: TaskDto) {
        val current = data.value.toMutableMap()
        val userTasks = current[userId]?.toMutableMap() ?: mutableMapOf()
        userTasks[taskDto.id] = taskDto
        current[userId] = userTasks
        data.value = current
    }

    /** Tarea guardada en memoria, o `null` si no existe. */
    fun storedTask(userId: String, taskId: String): TaskDto? = data.value[userId]?.get(taskId)

    override fun observeTasks(userId: String): Flow<List<TaskDto>> {
        failure?.let { throw it }
        return flow {
            tasksListenerCount++
            listenerFailure?.let { throw it }
            emitAll(data.map { map -> map[userId]?.values?.toList().orEmpty() })
        }
    }

    override fun observeTask(userId: String, taskId: String): Flow<TaskDto?> {
        failure?.let { throw it }
        return flow {
            taskListenerCount++
            listenerFailure?.let { throw it }
            emitAll(data.map { map -> map[userId]?.get(taskId) })
        }
    }

    override suspend fun getTask(userId: String, taskId: String): TaskDto? {
        failure?.let { throw it }
        return storedTask(userId, taskId)
    }

    override suspend fun saveTask(userId: String, taskDto: TaskDto) {
        failure?.let { throw it }
        givenTask(userId, taskDto)
        awaitServerConfirmation()
    }

    /** Fusiona [fields] sobre la tarea guardada; si no existe lanza [TaskError.TaskNotFound]. */
    override suspend fun updateTaskFields(userId: String, taskId: String, fields: Map<String, Any?>) {
        failure?.let { throw it }
        lastUpdatedFields = fields
        val stored = storedTask(userId, taskId) ?: throw TaskError.TaskNotFound
        givenTask(userId, TaskDto.fromMap(taskId, stored.toMap() + fields))
        awaitServerConfirmation()
    }

    override suspend fun deleteTask(userId: String, taskId: String) {
        failure?.let { throw it }
        val current = data.value.toMutableMap()
        val userTasks = current[userId]?.toMutableMap() ?: return
        userTasks.remove(taskId)
        current[userId] = userTasks
        data.value = current
        awaitServerConfirmation()
    }

    private suspend fun awaitServerConfirmation() {
        serverConfirmation?.await()
    }
}
