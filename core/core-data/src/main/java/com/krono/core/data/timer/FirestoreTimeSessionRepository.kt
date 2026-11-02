package com.krono.core.data.timer

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.task.FirestoreTaskErrorMapper
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

/** Implementación de [TimeSessionRepository] con Cloud Firestore (F-13): lee `timeSessions` del usuario. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTimeSessionRepository @Inject constructor(
    private val remoteDataSource: TimeSessionRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : TimeSessionRepository {

    /**
     * Emite las sesiones del intervalo para el usuario con sesión activa (lista vacía sin sesión).
     * Si el listener falla emite un [KronoResult.Error] y termina, en vez de fingir que no hay sesiones.
     */
    override fun observeSessionsBetween(from: Instant, to: Instant): Flow<KronoResult<List<TimeSession>>> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    remoteDataSource
                        .observeSessionsBetween(user.id, from.toEpochMilliseconds(), to.toEpochMilliseconds())
                        .map { documents -> documents.mapNotNull(TimeSessionDto::fromMap) }
                }
            }
            .map<List<TimeSession>, KronoResult<List<TimeSession>>> { KronoResult.Success(it) }
            .catch { emit(KronoResult.Error(FirestoreTaskErrorMapper.map(it))) }
}
