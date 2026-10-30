package com.krono.core.data.timer

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.firestore.awaitConfirmationOrQueued
import com.krono.core.data.task.FirestoreTaskErrorMapper
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishedSession
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimerRepository
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
 * Implementación de [TimerRepository] con Cloud Firestore (F-08, D-1): el cronómetro en
 * curso y las sesiones viven bajo `users/{uid}`, así sobreviven a cerrar la app (CA-3).
 *
 * Igual que las tareas, cada escritura espera la confirmación del servidor con
 * [awaitConfirmationOrQueued]: sin red se da por encolada y se informa como éxito, y los errores
 * que llegan antes del límite (permiso denegado, datos inválidos) se devuelven como
 * [KronoResult.Error] en vez de perderse.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTimerRepository @Inject constructor(
    private val remoteDataSource: TimerRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : TimerRepository {

    /** Emite el cronómetro en curso del usuario con sesión activa. */
    override fun observeActiveTimer(): Flow<ActiveTimer?> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    remoteDataSource.observeActiveTimer(user.id).map { it?.let(ActiveTimerDto::fromMap) }
                }
            }
            .catch { emit(null) }

    /** Lee una vez el cronómetro en curso. */
    override suspend fun getActiveTimer(): KronoResult<ActiveTimer?> = runCatchingTimer { userId ->
        remoteDataSource.getActiveTimer(userId)?.let(ActiveTimerDto::fromMap)
    }

    /** Guarda el cronómetro en curso. */
    override suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit> = runCatchingTimer { userId ->
        awaitConfirmationOrQueued { remoteDataSource.saveActiveTimer(userId, ActiveTimerDto.toMap(timer)) }
    }

    /**
     * Registra la sesión y la suma a la tarea. Si la tarea se eliminó mientras corría el
     * cronómetro, la sesión se conserva sin sumarse a nada y el resultado lo indica, para que la
     * UI avise al usuario en vez de perder el tiempo medido en silencio.
     *
     * La tarea puede borrarse entre la comprobación y el lote: entonces el `update` del lote
     * falla con `NOT_FOUND`, el lote entero se descarta y se repite sin sumar a la tarea. Como el
     * id de la sesión es estable, repetir el lote no la duplica.
     */
    override suspend fun finishActiveTimer(session: TimeSession): KronoResult<FinishedSession> =
        runCatchingTimer { userId ->
            val addedToTask = commitFinish(userId, session, addToTask = taskExistsOrAssumed(userId, session.taskId))
            FinishedSession(session, addedToTask)
        }

    /**
     * Indica si la tarea existe. Si no se puede comprobar (sin red y sin la tarea en caché) se
     * asume que existe: si no fuera así, el lote falla con `NOT_FOUND` y se repite sin sumar.
     */
    private suspend fun taskExistsOrAssumed(userId: String, taskId: String): Boolean = try {
        remoteDataSource.taskExists(userId, taskId)
    } catch (e: CancellationException) {
        throw e
    } catch (unreadable: Exception) {
        true
    }

    /** Escribe el lote de finalización; devuelve si el tiempo se sumó a la tarea. */
    private suspend fun commitFinish(userId: String, session: TimeSession, addToTask: Boolean): Boolean = try {
        writeFinish(userId, session, addToTask)
        addToTask
    } catch (e: CancellationException) {
        throw e
    } catch (error: Exception) {
        if (!addToTask || FirestoreTaskErrorMapper.map(error) != TaskError.TaskNotFound) throw error
        writeFinish(userId, session, addToTask = false)
        false
    }

    private suspend fun writeFinish(userId: String, session: TimeSession, addToTask: Boolean) {
        awaitConfirmationOrQueued {
            remoteDataSource.finishActiveTimer(
                userId = userId,
                session = TimeSessionDto.toMap(session),
                taskId = session.taskId,
                durationMillis = session.duration.inWholeMilliseconds,
                addToTask = addToTask,
            )
        }
    }

    private suspend fun <T> runCatchingTimer(block: suspend (userId: String) -> T): KronoResult<T> = try {
        val user = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser
        KronoResult.Success(block(user.id))
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        KronoResult.Error(FirestoreTaskErrorMapper.map(t))
    }
}
