package com.krono.feature.calendar.month.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.domain.task.TaskCategory
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.R
import com.krono.feature.calendar.month.CalendarDayUi
import kotlinx.datetime.LocalDate

/**
 * Rejilla del mes en una superficie de vidrio: cabecera con las iniciales de los días y una fila
 * por semana. Los días de los meses vecinos se atenúan; hoy lleva un anillo y el día
 * seleccionado, un fondo magenta. No lleva relleno lateral propio: el margen lo pone quien la usa,
 * para que cada día pueda medir 48 dp de ancho en pantallas de 360 dp.
 *
 * @param weeks Semanas de la rejilla.
 * @param onSelectDate Selecciona un día para ver su agenda.
 */
@Composable
internal fun MonthGrid(
    weeks: List<List<CalendarDayUi>>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(vertical = KronoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        WeekdayHeader(weeks.firstOrNull().orEmpty())
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day -> DayCell(day = day, onClick = { onSelectDate(day.date) }) }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(firstWeek: List<CalendarDayUi>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        firstWeek.forEach { day ->
            val fullName = day.date.dayOfWeek.fullName()
            Text(
                text = day.date.dayOfWeek.narrowName(),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = fullName },
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RowScope.DayCell(day: CalendarDayUi, onClick: () -> Unit) {
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
        DayNumber(day)
        DayIndicators(categories = day.categories, dimmed = !day.isInDisplayedMonth)
    }
}

@Composable
private fun DayNumber(day: CalendarDayUi) {
    val textColor = when {
        day.isSelected -> PrimaryFixedDim
        day.isInDisplayedMonth -> OnSurface
        else -> OnSurfaceVariant.copy(alpha = NEIGHBOR_MONTH_ALPHA)
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

@Composable
private fun DayIndicators(categories: List<TaskCategory>, dimmed: Boolean) {
    // Fila de alto fijo aunque no haya tareas, para que las celdas de la semana queden alineadas.
    Row(
        modifier = Modifier
            .padding(top = KronoSpacing.xs / 2)
            .heightIn(min = KronoSizes.calendarDayDot),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
    ) {
        categories.forEach { category ->
            val alpha = if (dimmed) NEIGHBOR_MONTH_ALPHA else FULL_ALPHA
            Box(
                modifier = Modifier
                    .size(KronoSizes.calendarDayDot)
                    .background(category.accentColor.copy(alpha = alpha), CircleShape),
            )
        }
    }
}

private const val NEIGHBOR_MONTH_ALPHA = 0.5f
private const val SELECTED_DAY_ALPHA = 0.2f
private const val FULL_ALPHA = 1f
