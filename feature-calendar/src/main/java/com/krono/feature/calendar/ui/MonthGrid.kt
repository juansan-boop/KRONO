package com.krono.feature.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.CalendarDayUi
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
