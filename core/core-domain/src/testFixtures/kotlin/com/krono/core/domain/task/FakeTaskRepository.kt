package com.krono.core.domain.task

import com.krono.core.common.KronoResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [TaskRepository], compartido vía test fixtures.
 *
 * - [failure]: si no es nulo, toda operación suspendida devuelve `KronoResult.Error(failure)`.
 * - [observeFailure]: si no es nulo, las observaciones emiten `KronoResult.Error` y terminan,
 *   como un listener de Firestore que falla.
 * - [gate]: si no es nulo, las operaciones suspendidas esperan a que se complete.
 */
class FakeTaskRepository : TaskRepository {

    private val tasksState = MutableStateFlow<Map<String, Task>>(emptyMap())

    /** Error forzado que devolverán las operaciones; `null` para funcionamiento normal. */
    var failure: Throwable? = null

    /** Error con el que fallan las observaciones nuevas; `null` para funcionamiento normal. */
    var observeFailure: Throwable? = null

    /** Compuerta para pausar operaciones asíncronas en pruebas de concurrencia. */
    var gate: CompletableDeferred<Unit>? = null

    /** Contador de invocaciones a [upsertTask]. */
    var upsertCalls: Int = 0
        private set

    /** Contador de invocaciones a [updateTaskFields]. */
    var updateFieldsCalls: Int = 0
        private set

    /** Campos pedidos en la última invocación a [updateTaskFields]. */
    var lastUpdatedFields: Set<TaskField> = emptySet()
        private set

    /** Contador de invocaciones a [deleteTask]. */
    var deleteCalls: Int = 0
        private set

    /** Contador de invocaciones a [getTaskById]. */
    var getTaskByIdCalls: Int = 0
        private set

    /** Cantidad de veces que se empezó a observar la lista de tareas (para probar reintentos). */
    var observeTasksSubscriptions: Int = 0
        private set

    /** Cantidad de veces que se empezó a observar una tarea puntual (para probar reintentos). */
    var observeTaskByIdSubscriptions: Int = 0
        private set

    /** Precarga tareas en el repositorio en memoria. */
    fun givenTasks(vararg tasks: Task) {
        val current = tasksState.value.toMutableMap()
        tasks.forEach { current[it.id] = it }
        tasksState.value = current
    }

    /** Tareas guardadas actualmente, para aserciones directas sin pasar por un Flow. */
    fun storedTasks(): List<Task> = tasksState.value.values.toList()

    /** Tarea guardada con [taskId], o `null` si no existe. */
    fun storedTask(taskId: String): Task? = tasksState.value[taskId]

    /** Emite la lista de tareas en memoria, o el error de [observeFailure]. */
    override fun observeTasks(): Flow<KronoResult<List<Task>>> = flow {
        observeTasksSubscriptions++
        val error = observeFailure
        if (error != null) {
            emit(KronoResult.Error(error))
        } else {
            emitAll(tasksState.map { KronoResult.Success(it.values.toList()) })
        }
    }

    /** Emite la tarea solicitada (o `null` si no existe), o el error de [observeFailure]. */
    override fun observeTaskById(taskId: String): Flow<KronoResult<Task?>> = flow {
        observeTaskByIdSubscriptions++
        val error = observeFailure
        if (error != null) {
            emit(KronoResult.Error(error))
        } else {
            emitAll(tasksState.map { KronoResult.Success(it[taskId]) })
        }
    }

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
        givenTasks(task)
        return KronoResult.Success(Unit)
    }

    /**
     * Copia solo los [fields] de [task] sobre la tarea guardada, como hace Firestore con una
     * escritura parcial; si no existe devuelve [TaskError.TaskNotFound].
     */
    override suspend fun updateTaskFields(task: Task, fields: Set<TaskField>): KronoResult<Unit> {
        updateFieldsCalls++
        lastUpdatedFields = fields
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        val stored = tasksState.value[task.id] ?: return KronoResult.Error(TaskError.TaskNotFound)
        givenTasks(fields.fold(stored) { merged, field -> merged.withFieldFrom(task, field) })
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

    private fun Task.withFieldFrom(source: Task, field: TaskField): Task = when (field) {
        TaskField.TITLE -> copy(title = source.title)
        TaskField.CATEGORY -> copy(category = source.category)
        TaskField.PRIORITY -> copy(priority = source.priority)
        TaskField.DUE_DATE -> copy(dueDate = source.dueDate)
        TaskField.ESTIMATED_DURATION -> copy(estimatedDuration = source.estimatedDuration)
        TaskField.IS_COMPLETED -> copy(isCompleted = source.isCompleted)
        TaskField.NOTE -> copy(note = source.note)
        TaskField.SUBTASKS -> copy(subtasks = source.subtasks)
    }
}
