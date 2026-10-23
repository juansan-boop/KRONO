package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlin.time.Duration

/** Observa el cronómetro en curso (F-08). */
class ObserveActiveTimerUseCase(
    private val repository: TimerRepository,
) {
    /** Emite el cronómetro en curso o `null` cuando no hay ninguno. */
    operator fun invoke(): Flow<ActiveTimer?> = repository.observeActiveTimer()
}

/**
 * Inicia el cronómetro de una tarea (F-08 CA-1).
 *
 * Si ya corre en esa misma tarea no hace nada, y si estaba en pausa lo reanuda. Si hay uno
 * en otra tarea devuelve [TimerError.AnotherTimerActive] para que la UI pida confirmación;
 * con `stopCurrent = true` finaliza y guarda el anterior antes de iniciar el nuevo (CA-4).
 */
class StartTimerUseCase(
    private val repository: TimerRepository,
    private val finishTimer: FinishTimerUseCase,
    private val clock: Clock,
) {
    /**
     * @param taskId tarea a medir.
     * @param stopCurrent confirma que se detenga el cronómetro de otra tarea.
     */
    suspend operator fun invoke(taskId: String, stopCurrent: Boolean = false): KronoResult<Unit> {
        val current = when (val result = repository.getActiveTimer()) {
            is KronoResult.Success -> result.data
            is KronoResult.Error -> return result
            KronoResult.Loading -> return KronoResult.Loading
        }
        return when {
            current?.taskId == taskId -> resumeIfPaused(current)
            current != null && !stopCurrent -> KronoResult.Error(TimerError.AnotherTimerActive(current.taskId))
            current != null -> finishThenStart(taskId)
            else -> startNew(taskId)
        }
    }

    private suspend fun resumeIfPaused(timer: ActiveTimer): KronoResult<Unit> =
        if (timer.isRunning) KronoResult.Success(Unit) else repository.saveActiveTimer(timer.copy(runningSince = clock.now()))

    private suspend fun finishThenStart(taskId: String): KronoResult<Unit> =
        when (val finished = finishTimer()) {
            is KronoResult.Success -> startNew(taskId)
            is KronoResult.Error -> finished
            KronoResult.Loading -> KronoResult.Loading
        }

    private suspend fun startNew(taskId: String): KronoResult<Unit> {
        val now = clock.now()
        return repository.saveActiveTimer(
            ActiveTimer(taskId = taskId, startedAt = now, runningSince = now, accumulated = Duration.ZERO),
        )
    }
}

/** Pausa el cronómetro en curso guardando el tiempo trabajado hasta ahora (F-08 CA-1). */
class PauseTimerUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
) {
    /** Pausa el cronómetro; si ya estaba en pausa no hace nada. */
    suspend operator fun invoke(): KronoResult<Unit> = updateActiveTimer(repository) { timer ->
        if (timer.isRunning) timer.copy(runningSince = null, accumulated = timer.elapsedAt(clock.now())) else null
    }
}

/** Reanuda el cronómetro en pausa (F-08 CA-1). */
class ResumeTimerUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
) {
    /** Reanuda el cronómetro; si ya estaba corriendo no hace nada. */
    suspend operator fun invoke(): KronoResult<Unit> = updateActiveTimer(repository) { timer ->
        if (timer.isRunning) null else timer.copy(runningSince = clock.now())
    }
}

/**
 * Finaliza el cronómetro en curso (F-08 CA-1, CA-2): registra una [TimeSession] de tipo
 * [TimeSessionType.CRONOMETRO] con el tiempo trabajado y lo suma a la tarea.
 */
class FinishTimerUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
    private val newSessionId: () -> String,
) {
    /** Devuelve la sesión registrada, o [TimerError.NoActiveTimer] si no había cronómetro. */
    suspend operator fun invoke(): KronoResult<TimeSession> {
        val timer = when (val result = repository.getActiveTimer()) {
            is KronoResult.Success -> result.data ?: return KronoResult.Error(TimerError.NoActiveTimer)
            is KronoResult.Error -> return result
            KronoResult.Loading -> return KronoResult.Loading
        }
        val now = clock.now()
        val session = TimeSession(
            id = newSessionId(),
            taskId = timer.taskId,
            startedAt = timer.startedAt,
            endedAt = now,
            duration = timer.elapsedAt(now),
            type = TimeSessionType.CRONOMETRO,
        )
        return when (val saved = repository.finishActiveTimer(session)) {
            is KronoResult.Success -> KronoResult.Success(session)
            is KronoResult.Error -> saved
            KronoResult.Loading -> KronoResult.Loading
        }
    }
}

/**
 * Lee el cronómetro en curso, le aplica [transform] y guarda el resultado. Si [transform]
 * devuelve `null` no hay nada que cambiar.
 */
private suspend fun updateActiveTimer(
    repository: TimerRepository,
    transform: (ActiveTimer) -> ActiveTimer?,
): KronoResult<Unit> {
    val timer = when (val result = repository.getActiveTimer()) {
        is KronoResult.Success -> result.data ?: return KronoResult.Error(TimerError.NoActiveTimer)
        is KronoResult.Error -> return result
        KronoResult.Loading -> return KronoResult.Loading
    }
    val updated = transform(timer) ?: return KronoResult.Success(Unit)
    return repository.saveActiveTimer(updated)
}
