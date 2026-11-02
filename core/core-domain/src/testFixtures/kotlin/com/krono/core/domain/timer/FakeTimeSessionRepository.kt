package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Doble de prueba en memoria de [TimeSessionRepository], compartido vía test fixtures.
 *
 * - [observeFailure]: si no es nulo, las observaciones emiten `KronoResult.Error` y terminan,
 *   como un listener de Firestore que falla.
 */
class FakeTimeSessionRepository : TimeSessionRepository {

    private val sessions = MutableStateFlow<List<TimeSession>>(emptyList())

    /** Error con el que fallan las observaciones nuevas; `null` para funcionamiento normal. */
    var observeFailure: Throwable? = null

    /** Reemplaza las sesiones guardadas. */
    fun givenSessions(vararg newSessions: TimeSession) {
        sessions.value = newSessions.toList()
    }

    override fun observeSessionsBetween(from: Instant, to: Instant): Flow<KronoResult<List<TimeSession>>> {
        val failure = observeFailure
        if (failure != null) return flow { emit(KronoResult.Error(failure)) }
        return sessions.map { list ->
            KronoResult.Success(list.filter { it.startedAt >= from && it.startedAt < to })
        }
    }
}
