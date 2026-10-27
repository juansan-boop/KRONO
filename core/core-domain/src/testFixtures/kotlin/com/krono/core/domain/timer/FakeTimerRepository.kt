package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Doble de prueba en memoria de [TimerRepository], compartido vía test fixtures.
 *
 * - [failure]: si no es nulo, toda operación suspendida devuelve `KronoResult.Error(failure)`.
 */
class FakeTimerRepository : TimerRepository {

    private val activeTimer = MutableStateFlow<ActiveTimer?>(null)

    /** Error forzado que devolverán las operaciones; `null` para funcionamiento normal. */
    var failure: Throwable? = null

    /** Sesiones registradas al finalizar, en orden. */
    val sessions = mutableListOf<TimeSession>()

    /** Tiempo real acumulado por tarea, como lo sumaría la persistencia (CA-2). */
    val actualDurationByTask = mutableMapOf<String, Duration>()

    /** Precarga un cronómetro en curso. */
    fun givenActiveTimer(timer: ActiveTimer?) {
        activeTimer.value = timer
    }

    /** Cronómetro en curso actual, para verificaciones. */
    val currentTimer: ActiveTimer? get() = activeTimer.value

    override fun observeActiveTimer(): Flow<ActiveTimer?> = activeTimer.asStateFlow()

    override suspend fun getActiveTimer(): KronoResult<ActiveTimer?> =
        failure?.let { KronoResult.Error(it) } ?: KronoResult.Success(activeTimer.value)

    override suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit> {
        failure?.let { return KronoResult.Error(it) }
        activeTimer.value = timer
        return KronoResult.Success(Unit)
    }

    override suspend fun finishActiveTimer(session: TimeSession): KronoResult<Unit> {
        failure?.let { return KronoResult.Error(it) }
        sessions += session
        actualDurationByTask[session.taskId] =
            (actualDurationByTask[session.taskId] ?: Duration.ZERO) + session.duration
        activeTimer.value = null
        return KronoResult.Success(Unit)
    }
}

/** Reloj de prueba que solo avanza cuando se le pide. */
class FakeClock(var current: Instant) : Clock {

    /** Avanza el reloj [duration]. */
    fun advanceBy(duration: Duration) {
        current += duration
    }

    override fun now(): Instant = current
}
