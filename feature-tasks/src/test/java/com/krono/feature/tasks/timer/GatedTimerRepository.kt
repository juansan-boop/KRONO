package com.krono.feature.tasks.timer

import com.krono.core.common.KronoResult
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FakeTimerRepository
import com.krono.core.domain.timer.FinishedSession
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimerRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Repositorio del cronómetro que delega en [delegate] pero cuyas escrituras pueden quedar
 * retenidas en [gate], como una escritura que todavía espera a Firestore. Sirve para reproducir
 * toques repetidos mientras una acción sigue en curso.
 */
class GatedTimerRepository(
    private val delegate: FakeTimerRepository,
) : TimerRepository by delegate {

    /** Si no es nulo, las escrituras esperan a que se complete antes de aplicarse. */
    var gate: CompletableDeferred<Unit>? = null

    /** Veces que se pidió guardar el cronómetro en curso. */
    var saveCalls: Int = 0
        private set

    /** Veces que se pidió finalizar el cronómetro. */
    var finishCalls: Int = 0
        private set

    override suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit> {
        saveCalls++
        gate?.await()
        return delegate.saveActiveTimer(timer)
    }

    override suspend fun finishActiveTimer(session: TimeSession): KronoResult<FinishedSession> {
        finishCalls++
        gate?.await()
        return delegate.finishActiveTimer(session)
    }
}
