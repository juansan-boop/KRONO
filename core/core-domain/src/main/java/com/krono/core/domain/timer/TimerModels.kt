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
 */
data class ActiveTimer(
    val taskId: String,
    val startedAt: Instant,
    val runningSince: Instant?,
    val accumulated: Duration,
) {
    /** El cronómetro está corriendo (no está en pausa). */
    val isRunning: Boolean get() = runningSince != null

    /** Tiempo trabajado hasta [now], sin contar las pausas. */
    fun elapsedAt(now: Instant): Duration {
        val currentStretch = runningSince?.let { (now - it).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO
        return accumulated + currentStretch
    }
}

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
}
