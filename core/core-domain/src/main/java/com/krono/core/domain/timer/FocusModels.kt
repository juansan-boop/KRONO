package com.krono.core.domain.timer

import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Constantes del modo enfoque (F-09). */
object FocusDefaults {
    /** Descanso que añade el botón "+5m Descanso", independiente del descanso de la plantilla. */
    val extraBreak: Duration = 5.minutes
}

/**
 * Descanso en curso dentro de un enfoque. Termina solo, al cumplirse [duration] desde [startedAt];
 * el momento se calcula con el reloj, así sobrevive a segundo plano y a cerrar la app.
 *
 * @property startedAt cuándo empezó el descanso.
 * @property duration cuánto dura.
 */
data class FocusBreak(
    val startedAt: Instant,
    val duration: Duration,
) {
    /** Instante en que el descanso termina. */
    val endsAt: Instant get() = startedAt + duration
}

/**
 * Parte del [ActiveTimer] que lo convierte en un enfoque con cuenta regresiva (F-09). Un cronómetro
 * (F-08) no tiene esta parte.
 *
 * @property blockDuration duración de un bloque de trabajo, tomada de la plantilla activa al iniciar.
 * @property breakDuration descanso que ofrece la plantilla al terminar un bloque.
 * @property targetElapsed tiempo trabajado acumulado con el que termina el bloque actual: crece en
 * [blockDuration] cada vez que se descansa tras completar un bloque, para encadenar otro.
 * @property breakInProgress descanso en curso o ya terminado que aún no se cerró, o `null`.
 */
data class FocusBlock(
    val blockDuration: Duration,
    val breakDuration: Duration,
    val targetElapsed: Duration = blockDuration,
    val breakInProgress: FocusBreak? = null,
)

/** Fase de un enfoque en un instante dado. */
enum class FocusPhase {
    /** Trabajando: la cuenta regresiva avanza. */
    WORKING,

    /** En pausa: el tiempo de trabajo no avanza. */
    PAUSED,

    /** En un descanso: el trabajo está detenido y no cuenta. */
    ON_BREAK,

    /** El descanso ya terminó y el usuario aún no vuelve al trabajo. */
    BREAK_OVER,

    /** La cuenta regresiva llegó a cero: hay que ofrecer el descanso o finalizar (CA-3). */
    BLOCK_COMPLETE,
}
