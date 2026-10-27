package com.krono.feature.tasks.timer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.krono.core.ui.theme.JetBrainsMono
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.feature.tasks.R
import kotlin.time.Duration

/** Ángulo desde el que arranca el anillo: las 12 en punto. */
private const val RING_START_ANGLE = -90f

/** Vuelta completa del anillo, en grados. */
private const val FULL_CIRCLE_DEGREES = 360f

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
    val description = stringResource(R.string.cd_timer_clock, clock)
    val progress = estimateProgress
        ?: ((elapsed.inWholeSeconds % SECONDS_PER_LAP).toFloat() / SECONDS_PER_LAP)

    Box(
        modifier = modifier
            .size(KronoSizes.timerRing)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = KronoSizes.timerRingStroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = size.copy(width = size.width - stroke.width, height = size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(SurfaceContainerHigh, 0f, FULL_CIRCLE_DEGREES, false, topLeft, arcSize, style = stroke)
            drawArc(
                PrimaryMagenta,
                RING_START_ANGLE,
                FULL_CIRCLE_DEGREES * progress,
                false,
                topLeft,
                arcSize,
                style = stroke,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Text(
                text = clock,
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMono),
                color = OnSurface,
            )
            Text(
                text = stringResource(R.string.timer_caption),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryFixedDim,
            )
        }
    }
}
