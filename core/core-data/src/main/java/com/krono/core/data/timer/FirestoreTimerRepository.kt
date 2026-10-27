package com.krono.core.data.timer

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.task.FirestoreTaskErrorMapper
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
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
        remoteDataSource.saveActiveTimer(userId, ActiveTimerDto.toMap(timer))
    }

    /**
     * Registra la sesión y la suma a la tarea. Si la tarea se eliminó mientras corría el
     * cronómetro, la sesión se guarda pero no se suma a nada.
     */
    override suspend fun finishActiveTimer(session: TimeSession): KronoResult<Unit> = runCatchingTimer { userId ->
        remoteDataSource.finishActiveTimer(
            userId = userId,
            session = TimeSessionDto.toMap(session),
            taskId = session.taskId,
            durationMillis = session.duration.inWholeMilliseconds,
            addToTask = remoteDataSource.taskExists(userId, session.taskId),
        )
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
