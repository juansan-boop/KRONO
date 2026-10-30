package com.krono.feature.tasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.krono.core.ui.theme.KronoSpacing
import kotlin.math.max

/** Margen sobre el ancho mínimo intrínseco de un texto antes de ponerlo junto a otro elemento. */
private const val MIN_WIDTH_SAFETY_FACTOR = 1.25f

/**
 * Contenido principal con un elemento al final (botón, insignia o acciones). El elemento se queda
 * a la derecha mientras el contenido conserve al menos su pieza indivisible más ancha (su ancho
 * mínimo intrínseco); si no, pasa debajo para que ninguna palabra se parta ni se pierda con
 * fuente grande o pantalla angosta. No depende de un umbral fijo de escala de fuente.
 *
 * Espera exactamente dos hijos: el contenido principal y el elemento final.
 *
 * @param spacing Separación entre ambos, horizontal en fila y vertical apilados.
 * @param stackedTrailingAtEnd Si, al apilarse, el elemento final se alinea a la derecha en vez de a la izquierda.
 */
@Composable
internal fun TrailingRowOrStack(
    modifier: Modifier = Modifier,
    spacing: Dp = KronoSpacing.sm,
    stackedTrailingAtEnd: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val (main, trailing) = measurables
        val spacingPx = spacing.roundToPx()
        val trailingWidth = trailing.maxIntrinsicWidth(Constraints.Infinity)
        // El ancho mínimo intrínseco de un texto subestima la palabra más larga (kerning, negritas),
        // así que se deja un margen para que la palabra no se parta al quedar justa.
        val mainMinWidth = (main.minIntrinsicWidth(Constraints.Infinity) * MIN_WIDTH_SAFETY_FACTOR).toInt()
        val fitsInRow = mainMinWidth + spacingPx + trailingWidth <= constraints.maxWidth
        if (fitsInRow) {
            val trailingPlaceable = trailing.measure(Constraints(maxWidth = trailingWidth))
            val mainPlaceable = main.measure(Constraints(maxWidth = constraints.maxWidth - trailingWidth - spacingPx))
            val height = max(mainPlaceable.height, trailingPlaceable.height)
            layout(constraints.maxWidth, height) {
                mainPlaceable.placeRelative(0, (height - mainPlaceable.height) / 2)
                trailingPlaceable.placeRelative(
                    constraints.maxWidth - trailingPlaceable.width,
                    (height - trailingPlaceable.height) / 2,
                )
            }
        } else {
            val mainPlaceable = main.measure(Constraints(maxWidth = constraints.maxWidth))
            val trailingPlaceable = trailing.measure(Constraints(maxWidth = constraints.maxWidth))
            layout(constraints.maxWidth, mainPlaceable.height + spacingPx + trailingPlaceable.height) {
                mainPlaceable.placeRelative(0, 0)
                val x = if (stackedTrailingAtEnd) constraints.maxWidth - trailingPlaceable.width else 0
                trailingPlaceable.placeRelative(x, mainPlaceable.height + spacingPx)
            }
        }
    }
}
