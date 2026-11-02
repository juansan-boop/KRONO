package com.krono.feature.tasks.timer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.JetBrainsMono
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh

/** Ángulo desde el que arranca el anillo: las 12 en punto. */
private const val RING_START_ANGLE = -90f

/** Vuelta completa del anillo, en grados. */
private const val FULL_CIRCLE_DEGREES = 360f

/** Factor por el que se achica el reloj cada vez que no cabe en el ancho del anillo. */
private const val CLOCK_SHRINK_FACTOR = 0.9f

/** Escala mínima del reloj; evita un bucle de reducción si el ancho disponible es casi cero. */
private const val CLOCK_MIN_SCALE = 0.25f

/**
 * Anillo con un reloj en el centro y un texto de apoyo debajo (diseño `modo_enfoque_krono_1`).
 * Lo comparten el cronómetro (F-08) y el modo enfoque (F-09).
 *
 * El reloj se reduce solo cuando, con la fuente muy grande, no cabe dentro del anillo: así nunca
 * se parte ni queda recortado.
 *
 * @param clock texto del reloj, por ejemplo "31:49".
 * @param progress fracción del arco coloreado, entre 0 y 1.
 * @param caption texto de apoyo bajo el reloj.
 * @param description descripción para lectores de pantalla; reemplaza a los textos visibles.
 */
@Composable
internal fun ClockRing(
    clock: String,
    progress: Float,
    caption: String,
    description: String,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier.padding(horizontal = KronoSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            FitClockText(clock)
            Text(
                text = caption,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryFixedDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Reloj de una sola línea que reduce su tamaño hasta caber en el ancho disponible. */
@Composable
private fun FitClockText(clock: String) {
    val fontScale = LocalDensity.current.fontScale
    var shrink by remember(clock.length, fontScale) { mutableFloatStateOf(1f) }
    val base = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMono)
    Text(
        text = clock,
        style = base.copy(fontSize = base.fontSize * shrink),
        color = OnSurface,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && shrink > CLOCK_MIN_SCALE) shrink *= CLOCK_SHRINK_FACTOR
        },
    )
}
