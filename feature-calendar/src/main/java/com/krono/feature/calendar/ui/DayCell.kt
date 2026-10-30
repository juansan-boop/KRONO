package com.krono.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.calendar.CalendarDayUi
import com.krono.feature.calendar.R

/**
 * Celda de un día, común a la rejilla mensual y a la franja de Semana y Día: número en un
 * círculo (anillo si es hoy, fondo magenta si está seleccionado) y puntos de categoría debajo.
 * Mide al menos 48 dp de alto y se reparte el ancho de la fila en partes iguales.
 *
 * @param day Día que representa.
 * @param onClick Selecciona el día.
 * @param label Texto sobre el número (abreviatura del día en la franja), o `null` para no mostrarlo.
 */
@Composable
internal fun RowScope.DayCell(day: CalendarDayUi, onClick: () -> Unit, label: String? = null) {
    val description = dayCellDescription(day)
    val todayState = stringResource(R.string.calendar_day_today_state)
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = KronoSizes.minTouchTarget)
            .selectable(selected = day.isSelected, onClick = onClick)
            .semantics {
                contentDescription = description
                if (day.isToday) stateDescription = todayState
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        label?.let { DayLabel(text = it, isSelected = day.isSelected) }
        DayNumber(day)
        DayIndicators(
            categories = day.categories,
            alpha = if (day.isInDisplayedPeriod) FULL_ALPHA else NEIGHBOR_PERIOD_ALPHA,
        )
    }
}

@Composable
private fun DayLabel(text: String, isSelected: Boolean) {
    // Sin límite de ancho: con la fuente al 200 % las tres letras superan los 48 dp de la celda y
    // preferimos que se centren y sobresalgan un poco antes que se recorten.
    Text(
        text = text,
        modifier = Modifier.wrapContentWidth(unbounded = true),
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) PrimaryFixedDim else OnSurfaceVariant,
        textAlign = TextAlign.Center,
        softWrap = false,
    )
}

@Composable
private fun DayNumber(day: CalendarDayUi) {
    val textColor = when {
        day.isSelected -> PrimaryFixedDim
        day.isInDisplayedPeriod -> OnSurface
        else -> OnSurfaceVariant.copy(alpha = NEIGHBOR_PERIOD_ALPHA)
    }
    val background = if (day.isSelected) PrimaryFixedDim.copy(alpha = SELECTED_DAY_ALPHA) else Color.Transparent
    val ring = if (day.isToday) PrimaryFixedDim else Color.Transparent
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = KronoSizes.calendarDayBadge, minHeight = KronoSizes.calendarDayBadge)
            .background(background, CircleShape)
            .border(KronoGlass.borderWidth, ring, CircleShape)
            .padding(KronoSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

private const val NEIGHBOR_PERIOD_ALPHA = 0.5f
private const val SELECTED_DAY_ALPHA = 0.2f
