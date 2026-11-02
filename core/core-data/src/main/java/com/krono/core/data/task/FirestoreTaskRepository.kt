package com.krono.core.data.task

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.firestore.awaitConfirmationOrQueued
import com.krono.core.data.timer.ActiveTimerDto
import com.krono.core.data.timer.TimerRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [TaskRepository] con Cloud Firestore (D-1, D-3, F-02).
 * Gestiona la sincronización offline, el mapeo de modelos y la traducción de errores a [KronoResult].
 *
 * Cada escritura espera la confirmación del servidor con [awaitConfirmationOrQueued]: sin red se
 * da por encolada y se informa como éxito (F-02 CA-1); los errores que llegan antes del límite
 * (validación local, permiso denegado) siguen devolviendo [KronoResult.Error].
 *
 * @param timerDataSource se consulta al eliminar una tarea para descartar su cronómetro en curso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskRepository @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
    private val timerDataSource: TimerRemoteDataSource,
) : TaskRepository {

    /**
     * Emite la lista de tareas de la sesión activa en tiempo real. Si el listener falla emite un
     * [KronoResult.Error] y termina, en vez de fingir una lista vacía.
     */
    override fun observeTasks(): Flow<KronoResult<List<Task>>> =
        observeForUser(signedOutValue = emptyList()) { user ->
            remoteDataSource.observeTasks(user.id).map { list -> list.map { it.toDomain() } }
        }

    /**
     * Emite los cambios de una tarea puntual en tiempo real (`null` si no existe). Si el listener
     * falla emite un [KronoResult.Error] y termina, en vez de fingir que la tarea no existe.
     */
    override fun observeTaskById(taskId: String): Flow<KronoResult<Task?>> =
        observeForUser(signedOutValue = null) { user ->
            remoteDataSource.observeTask(user.id, taskId).map { it?.toDomain() }
        }

    /** Consulta directa de una tarea puntual en Firestore. */
    override suspend fun getTaskById(taskId: String): KronoResult<Task> = runCatchingTask {
        val dto = remoteDataSource.getTask(requireUser().id, taskId) ?: throw TaskError.TaskNotFound
        dto.toDomain()
    }

    /** Crea o reemplaza una tarea completa en Firestore junto con sus subtareas embebidas. */
    override suspend fun upsertTask(task: Task): KronoResult<Unit> = runCatchingTask {
        val userId = requireUser().id
        awaitConfirmationOrQueued { remoteDataSource.saveTask(userId, task.toDto()) }
    }

    /** Escribe solo los [fields] de [task]; el tiempo real y los demás campos no se tocan. */
    override suspend fun updateTaskFields(task: Task, fields: Set<TaskField>): KronoResult<Unit> =
        runCatchingTask {
            val userId = requireUser().id
            val partial = task.toDto().toPartialMap(fields)
            awaitConfirmationOrQueued { remoteDataSource.updateTaskFields(userId, task.id, partial) }
        }

    /**
     * Elimina una tarea y sus subtareas embebidas en Firestore. Si el cronómetro en curso mide esa
     * tarea, se descarta en la misma escritura atómica para no dejar un cronómetro fantasma (F-07,
     * F-08). Las sesiones de tiempo ya registradas se conservan (ver [TaskRepository.deleteTask]).
     */
    override suspend fun deleteTask(taskId: String): KronoResult<Unit> = runCatchingTask {
        val userId = requireUser().id
        val discardActiveTimer = activeTimerTaskIdOrNull(userId) == taskId
        awaitConfirmationOrQueued { remoteDataSource.deleteTask(userId, taskId, discardActiveTimer) }
    }

    /**
     * Tarea que mide el cronómetro en curso, o `null` si no hay o no se puede leer. Una lectura
     * fallida (sin red y sin el documento en caché) no impide borrar la tarea: si el cronómetro
     * quedara huérfano, iniciar otro lo descarta sin pedir confirmación.
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
