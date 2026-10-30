package com.krono.core.domain.timer

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Cronómetro en curso del usuario (F-08). Solo existe uno a la vez (CA-4).
 *
 * El tiempo transcurrido se calcula siempre desde marcas de tiempo, no contando ticks en
 * memoria, para que no se pierda si la app pasa a segundo plano o se cierra (CA-3).
 *
 * @property taskId tarea que se está midiendo.
 * @property startedAt momento en que se inició por primera vez; inicio de la [TimeSession].
 * @property runningSince desde cuándo corre el tramo actual, o `null` si está en pausa.
 * @property accumulated tiempo trabajado en los tramos anteriores a [runningSince].
 * @property focus datos del modo enfoque (F-09), o `null` si es un cronómetro simple (F-08). Un
 * enfoque comparte el mismo documento, así que cuenta como el único cronómetro activo (CA-4).
 */
data class ActiveTimer(
    val taskId: String,
    val startedAt: Instant,
    val runningSince: Instant?,
    val accumulated: Duration,
    val focus: FocusBlock? = null,
) {
    /** El cronómetro está corriendo (no está en pausa). */
    val isRunning: Boolean get() = runningSince != null

    /**
     * Identificador de la [TimeSession] que produce este cronómetro al finalizar. Sale de la tarea
     * y de la marca de inicio, así que es el mismo en cada reintento: guardar dos veces la misma
     * sesión la reemplaza en vez de duplicarla (CA-2).
     */
    val sessionId: String get() = "$taskId$SESSION_ID_SEPARATOR${startedAt.toEpochMilliseconds()}"

    /**
     * Tiempo trabajado hasta [now], sin contar las pausas. En un enfoque nunca supera el final del
     * bloque: pasado el cero no se sigue sumando tiempo mientras nadie lo atiende.
     */
    fun elapsedAt(now: Instant): Duration {
        val currentStretch = runningSince?.let { (now - it).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO
        val elapsed = accumulated + currentStretch
        return focus?.let { elapsed.coerceAtMost(it.targetElapsed) } ?: elapsed
    }

    /** Tipo de [TimeSession] que produce al finalizar: `ENFOQUE` si es un enfoque, si no `CRONOMETRO`. */
    val sessionType: TimeSessionType
        get() = if (focus != null) TimeSessionType.ENFOQUE else TimeSessionType.CRONOMETRO

    /** Tiempo que falta para terminar el bloque de enfoque en [now], o `null` si no es un enfoque. */
    fun focusRemainingAt(now: Instant): Duration? =
        focus?.let { (it.targetElapsed - elapsedAt(now)).coerceAtLeast(Duration.ZERO) }

    /** Tiempo que falta para terminar el descanso en [now], o `null` si no hay descanso en curso. */
    fun breakRemainingAt(now: Instant): Duration? =
        focus?.breakInProgress?.let { (it.endsAt - now).coerceAtLeast(Duration.ZERO) }

    /** Fase del enfoque en [now], o `null` si no es un enfoque. */
    fun focusPhaseAt(now: Instant): FocusPhase? {
        val block = focus ?: return null
        val breakInProgress = block.breakInProgress
        return when {
            breakInProgress != null && now < breakInProgress.endsAt -> FocusPhase.ON_BREAK
            breakInProgress != null -> FocusPhase.BREAK_OVER
            !isRunning -> FocusPhase.PAUSED
            elapsedAt(now) >= block.targetElapsed -> FocusPhase.BLOCK_COMPLETE
            else -> FocusPhase.WORKING
        }
    }

    private companion object {
        /** Separa la tarea y la marca de inicio en [sessionId]; los ids de tarea (UUID) no lo contienen. */
        const val SESSION_ID_SEPARATOR = "_"
    }
}

/**
 * Resultado de finalizar el cronómetro (F-08 CA-2).
 *
 * @property session sesión registrada.
 * @property addedToTask `false` si la tarea ya no existía: la sesión se conservó, pero su tiempo
 * no se sumó a ninguna tarea y la UI debe avisarlo.
 */
data class FinishedSession(
    val session: TimeSession,
    val addedToTask: Boolean,
)

/** Origen de una sesión de tiempo (Sección 6 del SPEC). */
enum class TimeSessionType {
    /** Medida con el cronómetro (F-08). */
    CRONOMETRO,

    /** Medida en modo enfoque (F-09). */
    ENFOQUE,
}

/**
 * Tiempo real trabajado en una tarea. Es la fuente de verdad de la duración real:
 * `Task.actualDuration` es la suma de sus sesiones (Sección 6 del SPEC).
 *
 * @property duration tiempo trabajado, sin pausas; puede ser menor que `endedAt - startedAt`.
 */
data class TimeSession(
    val id: String,
    val taskId: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val duration: Duration,
    val type: TimeSessionType,
)

/** Errores de dominio del cronómetro (F-08). */
sealed class TimerError(message: String) : Exception(message) {

    /** Ya hay un cronómetro en curso en otra tarea; hay que confirmar antes de detenerlo (CA-4). */
    data class AnotherTimerActive(val activeTaskId: String) :
        TimerError("Ya tienes un cronómetro en curso en otra tarea.")

    /** Se intentó pausar, reanudar o finalizar sin un cronómetro en curso. */
    data object NoActiveTimer : TimerError("No hay un cronómetro en curso.")

    /** Se pidió un descanso, pero el cronómetro en curso no es un enfoque (F-09). */
    data object NotFocusSession : TimerError("El cronómetro en curso no es un enfoque.")
}
