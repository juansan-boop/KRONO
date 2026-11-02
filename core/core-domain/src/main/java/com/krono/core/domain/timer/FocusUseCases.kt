package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import com.krono.core.domain.focus.FocusTemplate
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Inicia un enfoque en una tarea con la plantilla indicada (F-09 CA-1). Comparte el documento y
 * las reglas del cronómetro: si hay otra sesión activa devuelve [TimerError.AnotherTimerActive] y,
 * con `stopCurrent = true`, la finaliza y guarda antes de empezar (F-08 CA-4).
 */
class StartFocusUseCase(
    private val startTimer: StartTimerUseCase,
) {
    /**
     * @param taskId tarea en la que se trabaja.
     * @param template plantilla cuyos minutos fijan el bloque y el descanso.
     * @param stopCurrent confirma que se detenga la sesión activa de otra tarea.
     */
    suspend operator fun invoke(
        taskId: String,
        template: FocusTemplate,
        stopCurrent: Boolean = false,
    ): KronoResult<Unit> {
        val block = template.focusMinutes.minutes
        val focus = FocusBlock(blockDuration = block, breakDuration = template.breakMinutes.minutes)
        return startTimer(taskId, stopCurrent, focus)
    }
}

/**
 * Empieza un descanso dentro del enfoque en curso (F-09 CA-2 y CA-3): detiene el trabajo, que no
 * cuenta mientras dura el descanso, y lo deja listo para reanudar cuando el usuario quiera.
 *
 * Si el bloque ya había llegado a cero, el descanso cierra ese bloque y deja preparado otro.
 * Si ya hay un descanso en curso no hace nada.
 */
class StartFocusBreakUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
) {
    /** Empieza un descanso de [duration]; [TimerError.NotFocusSession] si la sesión es un cronómetro. */
    suspend operator fun invoke(duration: Duration): KronoResult<Unit> {
        val timer = when (val result = repository.getActiveTimer()) {
            is KronoResult.Success -> result.data ?: return KronoResult.Error(TimerError.NoActiveTimer)
            is KronoResult.Error -> return result
            KronoResult.Loading -> return KronoResult.Loading
        }
        val now = clock.now()
        val block = timer.focus ?: return KronoResult.Error(TimerError.NotFocusSession)
        if (timer.focusPhaseAt(now) == FocusPhase.ON_BREAK) return KronoResult.Success(Unit)
        val closesBlock = timer.focusPhaseAt(now) == FocusPhase.BLOCK_COMPLETE
        val nextTarget = if (closesBlock) block.targetElapsed + block.blockDuration else block.targetElapsed
        return repository.saveActiveTimer(
            timer.copy(
                runningSince = null,
                accumulated = timer.elapsedAt(now),
                focus = block.copy(targetElapsed = nextTarget, breakInProgress = FocusBreak(now, duration)),
            ),
        )
    }
}
