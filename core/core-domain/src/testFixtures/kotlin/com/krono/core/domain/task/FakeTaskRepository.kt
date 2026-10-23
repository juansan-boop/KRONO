package com.krono.core.domain.task

import com.krono.core.common.KronoResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [TaskRepository], compartido vía test fixtures.
 *
 * - [failure]: si no es nulo, toda operación suspendida devuelve `KronoResult.Error(failure)`.
 * - [gate]: si no es nulo, las operaciones suspendidas esperan a que se complete.
 */
class FakeTaskRepository : TaskRepository {

    private val tasksState = MutableStateFlow<Map<String, Task>>(emptyMap())

    /** Error forzado que devolverán las operaciones; `null` para funcionamiento normal. */
    var failure: Throwable? = null

    /** Compuerta para pausar operaciones asíncronas en pruebas de concurrencia. */
    var gate: CompletableDeferred<Unit>? = null

    /** Contador de invocaciones a [upsertTask]. */
    var upsertCalls: Int = 0
        private set

    /** Contador de invocaciones a [deleteTask]. */
    var deleteCalls: Int = 0
        private set

    /** Contador de invocaciones a [getTaskById]. */
    var getTaskByIdCalls: Int = 0
        private set

    /** Precarga tareas en el repositorio en memoria. */
    fun givenTasks(vararg tasks: Task) {
        val current = tasksState.value.toMutableMap()
        tasks.forEach { current[it.id] = it }
        tasksState.value = current
    }

    /** Emite la lista ordenada de tareas en memoria. */
    override fun observeTasks(): Flow<List<Task>> =
        tasksState.map { it.values.toList() }

    /** Emite la tarea solicitada o `null` si no existe. */
    override fun observeTaskById(taskId: String): Flow<Task?> =
        tasksState.map { it[taskId] }

    /** Obtiene la tarea por identificador o devuelve [TaskError.TaskNotFound]. */
    override suspend fun getTaskById(taskId: String): KronoResult<Task> {
        getTaskByIdCalls++
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        val task = tasksState.value[taskId]
            ?: return KronoResult.Error(TaskError.TaskNotFound)
        return KronoResult.Success(task)
    }

    /** Agrega o actualiza la tarea en el mapa en memoria. */
    override suspend fun upsertTask(task: Task): KronoResult<Unit> {
        upsertCalls++
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        val current = tasksState.value.toMutableMap()
        current[task.id] = task
        tasksState.value = current
        return KronoResult.Success(Unit)
    }

    /** Elimina la tarea del mapa en memoria. */
    override suspend fun deleteTask(taskId: String): KronoResult<Unit> {
        deleteCalls++
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        val current = tasksState.value.toMutableMap()
        current.remove(taskId)
        tasksState.value = current
        return KronoResult.Success(Unit)
    }

    private suspend fun awaitGate() {
        gate?.await()
    }
}
