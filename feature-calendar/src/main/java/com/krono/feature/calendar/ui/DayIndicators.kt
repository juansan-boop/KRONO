package com.krono.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krono.core.domain.task.TaskCategory
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing

/**
 * Fila de puntos de categoría bajo un día de la rejilla mensual o de la franja semanal: un punto
 * por categoría con tareas ese día. Mantiene su alto aunque no haya puntos, para que las celdas de
 * una misma fila queden alineadas.
 *
 * @param categories Categorías con tareas ese día.
 * @param alpha Opacidad de los puntos (se atenúan en los días de los meses vecinos).
 */
@Composable
internal fun DayIndicators(categories: List<TaskCategory>, alpha: Float = FULL_ALPHA) {
    Row(
        modifier = Modifier
            .padding(top = KronoSpacing.xs / 2)
            .heightIn(min = KronoSizes.calendarDayDot),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
    ) {
        categories.forEach { category ->
            Box(
                modifier = Modifier
                    .size(KronoSizes.calendarDayDot)
                    .background(category.accentColor.copy(alpha = alpha), CircleShape),
            )
        }
    }
}

/** Opacidad completa de los puntos de categoría. */
internal const val FULL_ALPHA = 1f
