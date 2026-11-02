package com.krono.feature.tasks.timer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.krono.feature.tasks.R
import kotlin.time.Duration

/** Segundos por vuelta del anillo cuando la tarea no tiene estimación. */
private const val SECONDS_PER_LAP = 60L

/**
 * Anillo del cronómetro con el tiempo trabajado en el centro (diseño `modo_enfoque_krono_1`).
 *
 * Si la tarea tiene estimación, el anillo muestra qué parte ya se trabajó; si no, da una
 * vuelta por minuto para que se note que el cronómetro avanza.
 *
 * @param estimateProgress fracción de la estimación ya trabajada, o `null` si no hay estimación.
 */
@Composable
internal fun TimerRing(
    elapsed: Duration,
    estimateProgress: Float?,
    modifier: Modifier = Modifier,
) {
    val clock = formatClock(elapsed)
    val progress = estimateProgress
        ?: ((elapsed.inWholeSeconds % SECONDS_PER_LAP).toFloat() / SECONDS_PER_LAP)
    ClockRing(
        clock = clock,
        progress = progress,
        caption = stringResource(R.string.timer_caption),
        description = stringResource(R.string.cd_timer_clock, clock),
        modifier = modifier,
    )
}
