package com.krono.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoSpacing
import com.krono.feature.calendar.CalendarUiState
import kotlinx.datetime.LocalDate

/**
 * Acciones del calendario, agrupadas para no alargar la firma de la pantalla.
 *
 * @property onPrevious Muestra el período anterior (mes, semana o día, según la vista).
 * @property onNext Muestra el período siguiente.
 * @property onToday Vuelve al período y al día actuales.
 * @property onSelectMode Cambia entre Mes, Semana y Día.
 * @property onSelectDate Selecciona un día para ver su agenda.
 * @property onRetry Vuelve a leer las tareas tras un error.
 * @property onOpenTask Abre el detalle de una tarea.
 */
data class CalendarActions(
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onToday: () -> Unit = {},
    val onSelectMode: (CalendarViewMode) -> Unit = {},
    val onSelectDate: (LocalDate) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenTask: (String) -> Unit = {},
)

/**
 * Calendario sin estado en sus tres vistas (F-16 CA-1, CA-2 y CA-3): encabezado con navegación y
 * selector de vista, la rejilla del mes o la franja de la semana, y la agenda del día
 * seleccionado. Semana (`calendarweekscreen_vista_semanal_krono`) y Día
 * (`calendardayscreen_vista_diaria_krono`) comparten franja y agenda; Día añade el separador
 * "Ahora". Todo va en una sola lista desplazable para que nada se recorte con la fuente al 200 %.
 *
 * @param state Estado del calendario.
 * @param actions Acciones del usuario.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    // Margen lateral de todo menos la rejilla y la franja: ellas usan un margen menor para que cada
    // día llegue a 48 dp de ancho incluso en pantallas de 360 dp (7 x 48 = 336 dp).
    val contentMargin = Modifier.padding(horizontal = KronoSpacing.md)
    val daysMargin = Modifier.padding(horizontal = KronoSpacing.sm)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentPadding = PaddingValues(vertical = KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        item(key = HEADER_KEY) {
            CalendarHeader(
                mode = state.mode,
                title = calendarTitle(state),
                onPrevious = actions.onPrevious,
                onNext = actions.onNext,
                onToday = actions.onToday,
                onSelectMode = actions.onSelectMode,
                modifier = contentMargin,
            )
        }
        days(state = state, onSelectDate = actions.onSelectDate, modifier = daysMargin)
        dayAgenda(
            selectedDate = state.selectedDate,
            agenda = state.agenda,
            onOpenTask = actions.onOpenTask,
            onRetry = actions.onRetry,
            itemModifier = contentMargin,
        )
    }
}

/** Rejilla del mes en Mes, o franja de la semana en Semana y Día. */
private fun LazyListScope.days(state: CalendarUiState, onSelectDate: (LocalDate) -> Unit, modifier: Modifier) {
    item(key = DAYS_KEY) {
        when (state.mode) {
            CalendarViewMode.MONTH -> MonthGrid(weeks = state.weeks, onSelectDate = onSelectDate, modifier = modifier)
            CalendarViewMode.WEEK, CalendarViewMode.DAY ->
                DayStrip(week = state.weeks.single(), onSelectDate = onSelectDate, modifier = modifier)
        }
    }
}

private const val HEADER_KEY = "header"
private const val DAYS_KEY = "days"
