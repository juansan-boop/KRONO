package com.krono.feature.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.krono.core.ui.theme.KronoSpacing
import kotlin.math.max

/**
 * Fila de hijos de igual ancho que se apila en vertical cuando alguno no cabe en su parte sin
 * partir una palabra (fuente grande o pantalla angosta). Mide el ancho de la palabra más larga de
 * cada hijo, así el cambio no depende de un umbral fijo de escala de fuente.
 *
 * @param spacing Separación entre hijos, horizontal en fila y vertical apilados.
 */
@Composable
internal fun EqualWidthRowOrStack(
    modifier: Modifier = Modifier,
    spacing: Dp = KronoSpacing.sm,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val share = (constraints.maxWidth - spacingPx * (measurables.size - 1)) / measurables.size
        val fitsInRow = measurables.all { it.minIntrinsicWidth(Constraints.Infinity) <= share }
        if (fitsInRow) {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(share)) }
            layout(constraints.maxWidth, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (share + spacingPx), 0) }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(constraints.maxWidth)) }
            val height = placeables.sumOf { it.height } + spacingPx * (placeables.size - 1)
            layout(constraints.maxWidth, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + spacingPx
                }
            }
        }
    }
}

/**
 * Texto principal con una etiqueta al final (por ejemplo el chip de prioridad). La etiqueta se
 * queda a la derecha mientras el texto conserve al menos su palabra más larga; si no, pasa debajo
 * del texto para que ninguna palabra se parta ni se pierda.
 *
 * Espera exactamente dos hijos: el texto principal y la etiqueta.
 *
 * @param spacing Separación entre ambos, horizontal en fila y vertical apilados.
 */
@Composable
internal fun TrailingLabelRowOrStack(
    modifier: Modifier = Modifier,
    spacing: Dp = KronoSpacing.md,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val (main, label) = measurables
        val spacingPx = spacing.roundToPx()
        val labelWidth = label.maxIntrinsicWidth(Constraints.Infinity)
        val fitsInRow = main.minIntrinsicWidth(Constraints.Infinity) + spacingPx + labelWidth <= constraints.maxWidth
        if (fitsInRow) {
            val labelPlaceable = label.measure(Constraints(maxWidth = labelWidth))
            val mainPlaceable = main.measure(Constraints(maxWidth = constraints.maxWidth - labelWidth - spacingPx))
            val height = max(mainPlaceable.height, labelPlaceable.height)
            layout(constraints.maxWidth, height) {
                mainPlaceable.placeRelative(0, (height - mainPlaceable.height) / 2)
                labelPlaceable.placeRelative(constraints.maxWidth - labelPlaceable.width, (height - labelPlaceable.height) / 2)
            }
        } else {
            val mainPlaceable = main.measure(Constraints(maxWidth = constraints.maxWidth))
            val labelPlaceable = label.measure(Constraints(maxWidth = constraints.maxWidth))
            layout(constraints.maxWidth, mainPlaceable.height + spacingPx + labelPlaceable.height) {
                mainPlaceable.placeRelative(0, 0)
                labelPlaceable.placeRelative(0, mainPlaceable.height + spacingPx)
            }
        }
    }
}
