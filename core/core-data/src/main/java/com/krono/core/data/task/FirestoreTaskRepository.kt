package com.krono.core.data.task

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.firestore.WriteConfirmationResult
import com.krono.core.data.firestore.awaitConfirmationOrQueued
import com.krono.core.data.local.room.TaskDao
import com.krono.core.data.local.room.TaskEntity
import com.krono.core.data.local.room.toEntity
import com.krono.core.data.timer.ActiveTimerDto
import com.krono.core.data.timer.TimerRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.sync.SyncStatus
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [TaskRepository] con respaldo dual: Cloud Firestore como fuente remota
 * y [TaskDao] (Room) como base de datos local estructurada (D-1, D-3, F-02).
 *
 * Garantiza recuperación estructurada de tareas y subtareas al reiniciar la aplicación
 * sin conexión de red, manteniendo además la sincronización reactiva en tiempo real.
 *
 * @param remoteDataSource Fuente de datos remota en Cloud Firestore.
 * @param authDataSource Proveedor de estado de autenticación del usuario actual.
 * @param timerDataSource Fuente de datos de temporizadores para descarte de cronómetro activo al borrar tareas.
 * @param taskDao DAO de Room para almacenamiento estructurado local offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskRepository @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
    private val timerDataSource: TimerRemoteDataSource,
    private val taskDao: TaskDao,
) : TaskRepository {

    /**
     * Constructor secundario para pruebas unitarias sin persistencia física de Room.
     */
    constructor(
        remoteDataSource: TaskRemoteDataSource,
        authDataSource: AuthRemoteDataSource,
        timerDataSource: TimerRemoteDataSource,
    ) : this(remoteDataSource, authDataSource, timerDataSource, NoOpTaskDao)

    /**
     * Emite la lista de tareas de la sesión activa en tiempo real.
     * Persiste automáticamente en Room cada emisión remota y, si la observación remota falla,
     * intenta recuperar la última caché estructurada local disponible.
     */
    override fun observeTasks(): Flow<KronoResult<List<Task>>> =
        observeForUser(signedOutValue = emptyList()) { user ->
            remoteDataSource.observeTasks(user.id)
                .onEach { dtoList ->
                    val domainTasks = dtoList.map { it.toDomain().copy(syncStatus = SyncStatus.CONFIRMED) }
                    taskDao.upsertTasks(domainTasks.map { it.toEntity(user.id, TaskEntity.STATUS_CONFIRMED) })
                }
                .map { list -> list.map { it.toDomain().copy(syncStatus = SyncStatus.CONFIRMED) } }
                .catch { remoteError ->
                    val cachedTasks = taskDao.getTasks(user.id)
                    if (cachedTasks.isNotEmpty()) {
                        emit(cachedTasks.map { it.toDomain() })
                    } else {
                        throw remoteError
                    }
                }
        }

    /**
     * Emite los cambios de una tarea puntual en tiempo real (`null` si no existe).
     * Si la observación remota falla, recurre a la tarea almacenada en Room si existe.
     */
    override fun observeTaskById(taskId: String): Flow<KronoResult<Task?>> =
        observeForUser(signedOutValue = null) { user ->
            remoteDataSource.observeTask(user.id, taskId)
                .onEach { dto ->
                    if (dto != null) {
                        val domain = dto.toDomain().copy(syncStatus = SyncStatus.CONFIRMED)
                        taskDao.upsertTask(domain.toEntity(user.id, TaskEntity.STATUS_CONFIRMED))
                    }
                }
                .map { it?.toDomain()?.copy(syncStatus = SyncStatus.CONFIRMED) }
                .catch { remoteError ->
                    val cached = taskDao.getTaskById(user.id, taskId)
                    if (cached != null) {
                        emit(cached.toDomain())
                    } else {
                        throw remoteError
                    }
                }
        }

    /**
     * Consulta directa de una tarea puntual en Firestore con respaldo en la caché de Room.
     */
    override suspend fun getTaskById(taskId: String): KronoResult<Task> = runCatchingTask {
        val userId = requireUser().id
        try {
            val dto = remoteDataSource.getTask(userId, taskId)
            if (dto != null) {
                val domain = dto.toDomain().copy(syncStatus = SyncStatus.CONFIRMED)
                taskDao.upsertTask(domain.toEntity(userId, TaskEntity.STATUS_CONFIRMED))
                domain
            } else {
                val cached = taskDao.getTaskById(userId, taskId)
                cached?.toDomain() ?: throw TaskError.TaskNotFound
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = taskDao.getTaskById(userId, taskId)
            cached?.toDomain() ?: throw e
        }
    }

    /**
     * Crea o reemplaza una tarea completa junto con sus subtareas embebidas,
     * respaldándola inmediatamente en Room antes y después de confirmar con el servidor.
     */
    override suspend fun upsertTask(task: Task): KronoResult<Unit> = runCatchingTask {
        val userId = requireUser().id
        taskDao.upsertTask(task.toEntity(userId, TaskEntity.STATUS_PENDING_SYNC))
        val confirmation = try {
            awaitConfirmationOrQueued { remoteDataSource.saveTask(userId, task.toDto()) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            taskDao.upsertTask(task.toEntity(userId, TaskEntity.STATUS_FAILED))
            throw t
        }

        if (confirmation is WriteConfirmationResult.Confirmed) {
            taskDao.upsertTask(task.toEntity(userId, TaskEntity.STATUS_CONFIRMED))
        }
    }

    /**
     * Escribe solo los [fields] indicados de [task] sobre la tarea guardada,
     * actualizando tanto el documento en Firestore como el registro correspondiente en Room.
     */
    override suspend fun updateTaskFields(task: Task, fields: Set<TaskField>): KronoResult<Unit> =
        runCatchingTask {
            val userId = requireUser().id
            val currentCached = taskDao.getTaskById(userId, task.id)
            val baseTask = currentCached?.toDomain() ?: task
            var updatedTask = baseTask
            if (TaskField.TITLE in fields) updatedTask = updatedTask.copy(title = task.title)
            if (TaskField.CATEGORY in fields) updatedTask = updatedTask.copy(category = task.category)
            if (TaskField.PRIORITY in fields) updatedTask = updatedTask.copy(priority = task.priority)
            if (TaskField.DUE_DATE in fields) updatedTask = updatedTask.copy(dueDate = task.dueDate)
            if (TaskField.ESTIMATED_DURATION in fields) updatedTask = updatedTask.copy(estimatedDuration = task.estimatedDuration)
            if (TaskField.IS_COMPLETED in fields) updatedTask = updatedTask.copy(isCompleted = task.isCompleted)
            if (TaskField.NOTE in fields) updatedTask = updatedTask.copy(note = task.note)
            if (TaskField.SUBTASKS in fields) updatedTask = updatedTask.copy(subtasks = task.subtasks)

            taskDao.upsertTask(updatedTask.toEntity(userId, TaskEntity.STATUS_PENDING_SYNC))

            val partial = task.toDto().toPartialMap(fields)
            val confirmation = try {
                awaitConfirmationOrQueued { remoteDataSource.updateTaskFields(userId, task.id, partial) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                taskDao.upsertTask(updatedTask.toEntity(userId, TaskEntity.STATUS_FAILED))
                throw t
            }

            if (confirmation is WriteConfirmationResult.Confirmed) {
                taskDao.upsertTask(updatedTask.toEntity(userId, TaskEntity.STATUS_CONFIRMED))
            }
        }

    /**
     * Elimina una tarea y sus subtareas embebidas de Firestore y de la caché de Room.
     */
    override suspend fun deleteTask(taskId: String): KronoResult<Unit> = runCatchingTask {
        val userId = requireUser().id
        val discardActiveTimer = activeTimerTaskIdOrNull(userId) == taskId
        taskDao.deleteTask(userId, taskId)
        awaitConfirmationOrQueued { remoteDataSource.deleteTask(userId, taskId, discardActiveTimer) }
    }

    /**
     * Tarea que mide el cronómetro en curso, o `null` si no hay o no se puede leer.
     */
    private suspend fun activeTimerTaskIdOrNull(userId: String): String? = try {
        timerDataSource.getActiveTimer(userId)?.let(ActiveTimerDto::fromMap)?.taskId
    } catch (e: CancellationException) {
        throw e
    } catch (unreadable: Exception) {
        null
    }

    private fun <T> observeForUser(
        signedOutValue: T,
        observe: (User) -> Flow<T>,
    ): Flow<KronoResult<T>> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) flowOf(signedOutValue) else observe(user)
            }
            .map<T, KronoResult<T>> { KronoResult.Success(it) }
            .catch { emit(KronoResult.Error(FirestoreTaskErrorMapper.map(it))) }

    private fun requireUser(): User = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser

    private suspend fun <T> runCatchingTask(block: suspend () -> T): KronoResult<T> = try {
        KronoResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        KronoResult.Error(FirestoreTaskErrorMapper.map(t))
    }
}

/**
 * Implementación no operativa de [TaskDao] empleada en pruebas donde no se requiere
 * instanciar una base de datos SQLite en disco.
 */
internal object NoOpTaskDao : TaskDao {
    override fun observeTasks(userId: String): Flow<List<TaskEntity>> = emptyFlow()
    override fun observeTaskById(userId: String, taskId: String): Flow<TaskEntity?> = emptyFlow()
    override suspend fun getTasks(userId: String): List<TaskEntity> = emptyList()
    override suspend fun getTaskById(userId: String, taskId: String): TaskEntity? = null
    override suspend fun upsertTask(task: TaskEntity) {}
    override suspend fun upsertTasks(tasks: List<TaskEntity>) {}
    override suspend fun getPendingSyncTasks(userId: String): List<TaskEntity> = emptyList()
    override suspend fun clearTasksForUser(userId: String) {}
    override suspend fun deleteTask(userId: String, taskId: String) {}
}
