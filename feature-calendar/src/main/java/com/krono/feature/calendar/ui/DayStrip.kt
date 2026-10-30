package com.krono.feature.calendar.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.CalendarDayUi
import com.krono.feature.calendar.R
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Franja de los siete días de la semana (LUN a DOM) de las vistas Semana y Día, en una superficie
 * de vidrio: abreviatura, número y puntos de categoría de cada día, con el seleccionado resaltado.
 * Como la rejilla mensual, no lleva relleno lateral propio para que cada día mida 48 dp de ancho
 * en pantallas de 360 dp. Si las abreviaturas de tres letras no caben en su celda con espacio entre
 * ellas (por ejemplo con la fuente al 200 %), todas pasan a una sola letra para que no se peguen.
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(vertical = KronoSpacing.sm)
            .semantics { contentDescription = description }
            .selectableGroup(),
    ) {
        val useInitials = !shortNamesFit(cellWidth = maxWidth / week.size)
        Row {
            week.forEach { day ->
                val dayOfWeek = day.date.dayOfWeek
                DayCell(
                    day = day,
                    onClick = { onSelectDate(day.date) },
                    label = if (useInitials) dayOfWeek.narrowName() else dayOfWeek.shortName(),
                )
            }
        }
    }
}

/**
 * Indica si la abreviatura más ancha de la franja cabe en una celda de [cellWidth] dejando
 * [KronoSpacing.xs] libres a cada lado, con la escala de fuente vigente.
 */
@Composable
private fun shortNamesFit(cellWidth: Dp): Boolean {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val widestPx = DayOfWeek.entries.maxOf { measurer.measure(it.shortName(), style, softWrap = false).size.width }
    return with(LocalDensity.current) { widestPx <= (cellWidth - KronoSpacing.xs * 2).toPx() }
}
