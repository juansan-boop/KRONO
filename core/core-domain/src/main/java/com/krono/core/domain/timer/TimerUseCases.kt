package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskRepository
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
 *
 * Si el cronómetro en curso mide una tarea que ya no existe (por ejemplo, borrada desde otro
 * dispositivo), es un cronómetro huérfano: se descarta sin pedir confirmación ni guardar su
 * sesión, porque el tiempo de una tarea borrada ya no cuenta (F-07 CA-4). Si no se puede
 * comprobar la tarea, se asume que existe y se pide confirmación como siempre.
 */
class StartTimerUseCase(
    private val repository: TimerRepository,
    private val taskRepository: TaskRepository,
    private val finishTimer: FinishTimerUseCase,
    private val clock: Clock,
) {
    /**
     * @param taskId tarea a medir.
     * @param stopCurrent confirma que se detenga el cronómetro de otra tarea.
     * @param focus datos del enfoque (F-09) si se inicia un enfoque en vez de un cronómetro simple.
     * Un cronómetro y un enfoque son modos distintos aunque sean de la misma tarea: pasar de uno a
     * otro también pide confirmación.
     */
    suspend operator fun invoke(
        taskId: String,
        stopCurrent: Boolean = false,
        focus: FocusBlock? = null,
    ): KronoResult<Unit> {
        val current = when (val result = repository.getActiveTimer()) {
            is KronoResult.Success -> result.data
            is KronoResult.Error -> return result
            KronoResult.Loading -> return KronoResult.Loading
        }
        return when {
            current != null && current.taskId == taskId && (current.focus != null) == (focus != null) ->
                keepOrResume(current)
            current != null && isOrphan(current) -> startNew(taskId, focus)
            current != null && !stopCurrent -> KronoResult.Error(TimerError.AnotherTimerActive(current.taskId))
            current != null -> finishThenStart(taskId, focus)
            else -> startNew(taskId, focus)
        }
    }

    /** El cronómetro mide una tarea que ya no existe; cualquier otro error no lo hace huérfano. */
    private suspend fun isOrphan(timer: ActiveTimer): Boolean {
        val task = taskRepository.getTaskById(timer.taskId)
        return task is KronoResult.Error && task.throwable == TaskError.TaskNotFound
    }

    /**
     * Ya hay una sesión de este mismo modo en esta tarea. Un cronómetro en pausa se reanuda; un
     * enfoque se deja tal cual, porque puede estar en un descanso que reanudarlo cortaría.
     */
    private suspend fun keepOrResume(timer: ActiveTimer): KronoResult<Unit> =
        if (timer.isRunning || timer.focus != null) {
            KronoResult.Success(Unit)
        } else {
            repository.saveActiveTimer(timer.copy(runningSince = clock.now()))
        }

    private suspend fun finishThenStart(taskId: String, focus: FocusBlock?): KronoResult<Unit> =
        when (val finished = finishTimer()) {
            is KronoResult.Success -> startNew(taskId, focus)
            is KronoResult.Error -> finished
            KronoResult.Loading -> KronoResult.Loading
        }

    /** Guarda un cronómetro nuevo; reemplaza el documento anterior, así que también descarta un huérfano. */
    private suspend fun startNew(taskId: String, focus: FocusBlock?): KronoResult<Unit> {
        val now = clock.now()
        return repository.saveActiveTimer(
            ActiveTimer(taskId = taskId, startedAt = now, runningSince = now, accumulated = Duration.ZERO, focus = focus),
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

/**
 * Reanuda el cronómetro en pausa (F-08 CA-1). En un enfoque, reanudar durante un descanso (o
 * después de él) lo cierra y vuelve al trabajo (F-09 CA-2).
 */
class ResumeTimerUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
) {
    /** Reanuda el cronómetro; si ya estaba corriendo no hace nada. */
    suspend operator fun invoke(): KronoResult<Unit> = updateActiveTimer(repository) { timer ->
        if (timer.isRunning) null else timer.copy(runningSince = clock.now(), focus = timer.focus?.copy(breakInProgress = null))
    }
}

/**
 * Finaliza el cronómetro en curso (F-08 CA-1, CA-2): registra una [TimeSession] con el tiempo
 * trabajado y lo suma a la tarea. El tipo es [TimeSessionType.ENFOQUE] si el cronómetro era un
 * enfoque (F-09 CA-2) y [TimeSessionType.CRONOMETRO] si no. Las pausas y los descansos no cuentan.
 *
 * El id de la sesión es [ActiveTimer.sessionId]: si una escritura falla y el usuario reintenta,
 * se guarda la misma sesión en vez de una segunda.
 */
class FinishTimerUseCase(
    private val repository: TimerRepository,
    private val clock: Clock,
) {
    /**
     * Devuelve la sesión registrada e indica si se sumó a la tarea (no se suma si la tarea ya no
     * existía), o [TimerError.NoActiveTimer] si no había cronómetro.
     */
    suspend operator fun invoke(): KronoResult<FinishedSession> {
        val timer = when (val result = repository.getActiveTimer()) {
            is KronoResult.Success -> result.data ?: return KronoResult.Error(TimerError.NoActiveTimer)
            is KronoResult.Error -> return result
            KronoResult.Loading -> return KronoResult.Loading
        }
        val now = clock.now()
        val session = TimeSession(
            id = timer.sessionId,
            taskId = timer.taskId,
            startedAt = timer.startedAt,
            endedAt = now,
            duration = timer.elapsedAt(now),
            type = timer.sessionType,
        )
        return repository.finishActiveTimer(session)
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
