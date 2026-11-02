package com.krono.feature.tasks.testing

import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.task.FirestoreTaskRepository
import com.krono.core.data.task.TaskDto
import com.krono.core.data.task.TaskRemoteDataSource
import com.krono.core.data.timer.TimerRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.Task
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fuente remota de tareas que se comporta como Firestore sin conexión: aplica cada escritura en
 * su caché en memoria al instante, pero la confirmación del servidor nunca llega.
 */
class OfflineTaskRemoteDataSource : TaskRemoteDataSource {

    private val localCache = MutableStateFlow<Map<String, TaskDto>>(emptyMap())

    /** Precarga una tarea en la caché local. */
    fun givenTask(task: Task) {
        localCache.update { it + (task.id to TaskDto.fromDomain(task)) }
    }

    /** Tarea en la caché local convertida a dominio, o `null` si no existe. */
    fun cachedTask(taskId: String): Task? = localCache.value[taskId]?.toDomain()

    override fun observeTasks(userId: String): Flow<List<TaskDto>> = localCache.map { it.values.toList() }

    override fun observeTask(userId: String, taskId: String): Flow<TaskDto?> = localCache.map { it[taskId] }

    override suspend fun getTask(userId: String, taskId: String): TaskDto? = localCache.value[taskId]

    override suspend fun saveTask(userId: String, taskDto: TaskDto) {
        localCache.update { it + (taskDto.id to taskDto) }
        awaitCancellation()
    }

    override suspend fun updateTaskFields(userId: String, taskId: String, fields: Map<String, Any?>) {
        localCache.update { cache ->
            val stored = cache.getValue(taskId)
            cache + (taskId to TaskDto.fromMap(taskId, stored.toMap() + fields))
        }
        awaitCancellation()
    }

    override suspend fun deleteTask(userId: String, taskId: String, discardActiveTimer: Boolean) {
        localCache.update { it - taskId }
        awaitCancellation()
    }
}

/** Sesión fija de un usuario autenticado; solo expone lo que usa el repositorio de tareas. */
class SignedInAuthDataSource : AuthRemoteDataSource {

    private val user = User(id = "uid-offline", email = "sin.red@krono.app")

    override fun observeUser(): Flow<User?> = MutableStateFlow(user)

    override val currentUser: User = user

    override suspend fun signIn(email: String, password: String): User = unsupported()

    override suspend fun createAccount(email: String, password: String): User = unsupported()

    override suspend fun sendPasswordReset(email: String) = unsupported()

    override fun signOut() = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("No se usa en estas pruebas")
}

/** Fuente del cronómetro sin ningún cronómetro en curso; solo expone lo que usa el repositorio de tareas. */
class NoActiveTimerDataSource : TimerRemoteDataSource {

    override fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?> = flowOf(null)

    override suspend fun getActiveTimer(userId: String): Map<String, Any?>? = null

    override suspend fun saveActiveTimer(userId: String, timer: Map<String, Any?>) = unsupported()

    override suspend fun deleteActiveTimer(userId: String) = unsupported()

    override suspend fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
    ): Boolean = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("No se usa en estas pruebas")
}

/** Repositorio real de tareas conectado a [remote] sin red y a una sesión activa. */
fun offlineTaskRepository(remote: OfflineTaskRemoteDataSource): FirestoreTaskRepository =
    FirestoreTaskRepository(remote, SignedInAuthDataSource(), NoActiveTimerDataSource())
