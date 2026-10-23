package com.krono.core.data.task

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskError
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskRepository @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : TaskRepository {

    /** Emite la lista reactiva de tareas asociadas a la sesión activa en tiempo real. */
    override fun observeTasks(): Flow<List<Task>> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    remoteDataSource.observeTasks(user.id)
                        .map { list -> list.map { it.toDomain() } }
                }
            }
            .catch { emit(emptyList()) }

    /** Emite los cambios de una tarea puntual en tiempo real. */
    override fun observeTaskById(taskId: String): Flow<Task?> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    remoteDataSource.observeTask(user.id, taskId)
                        .map { it?.toDomain() }
                }
            }
            .catch { emit(null) }

    /** Consulta directa de una tarea puntual en Firestore. */
    override suspend fun getTaskById(taskId: String): KronoResult<Task> = runCatchingTask {
        val user = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser
        val dto = remoteDataSource.getTask(user.id, taskId) ?: throw TaskError.TaskNotFound
        dto.toDomain()
    }

    /** Crea o actualiza una tarea en Firestore junto con sus subtareas embebidas. */
    override suspend fun upsertTask(task: Task): KronoResult<Unit> = runCatchingTask {
        val user = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser
        remoteDataSource.saveTask(user.id, task.toDto())
    }

    /** Elimina una tarea y sus subtareas embebidas en Firestore. */
    override suspend fun deleteTask(taskId: String): KronoResult<Unit> = runCatchingTask {
        val user = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser
        remoteDataSource.deleteTask(user.id, taskId)
    }

    private suspend fun <T> runCatchingTask(block: suspend () -> T): KronoResult<T> = try {
        KronoResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        KronoResult.Error(FirestoreTaskErrorMapper.map(t))
    }
}
