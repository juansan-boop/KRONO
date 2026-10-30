package com.krono.feature.calendar.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.CalendarDayUi
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDate

/**
 * Franja de los siete días de la semana (LUN a DOM) de las vistas Semana y Día, en una superficie
 * de vidrio: abreviatura, número y puntos de categoría de cada día, con el seleccionado resaltado.
 * Como la rejilla mensual, no lleva relleno lateral propio para que cada día mida 48 dp de ancho
 * en pantallas de 360 dp.
 *
 * @param week Los siete días de la semana, de lunes a domingo.
 * @param onSelectDate Selecciona un día para ver su agenda.
 */
@Composable
internal fun DayStrip(
    week: List<CalendarDayUi>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.calendar_week_strip)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(vertical = KronoSpacing.sm)
            .semantics { contentDescription = description }
            .selectableGroup(),
    ) {
        week.forEach { day ->
            DayCell(day = day, onClick = { onSelectDate(day.date) }, label = day.date.dayOfWeek.shortName())
        }
    }
}
