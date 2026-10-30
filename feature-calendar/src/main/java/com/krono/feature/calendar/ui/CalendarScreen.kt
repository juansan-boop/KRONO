package com.krono.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoSpacing
import com.krono.feature.calendar.CalendarUiState
import kotlinx.datetime.LocalDate

/**
 * Acciones de la vista mensual, agrupadas para no alargar la firma de la pantalla.
 *
 * @property onPreviousMonth Muestra el mes anterior.
 * @property onNextMonth Muestra el mes siguiente.
 * @property onToday Vuelve al mes y al día actuales.
 * @property onSelectDate Selecciona un día para ver su agenda.
 * @property onRetry Vuelve a leer las tareas tras un error.
 * @property onOpenTask Abre el detalle de una tarea.
 */
data class CalendarActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onToday: () -> Unit = {},
    val onSelectDate: (LocalDate) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenTask: (String) -> Unit = {},
)

/**
 * Vista mensual del calendario sin estado, según `calendarmonthscreen_vista_mensual_y_agenda`:
 * encabezado con navegación, rejilla del mes y agenda del día seleccionado. Todo va en una sola
 * lista desplazable para que nada se recorte con la fuente al 200 %.
 *
 * @param state Estado de la vista.
 * @param actions Acciones del usuario.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    // Margen lateral de todo menos la rejilla: ella usa un margen menor para que cada día llegue a 48 dp
    // de ancho incluso en pantallas de 360 dp (7 x 48 = 336 dp).
    val contentMargin = Modifier.padding(horizontal = KronoSpacing.md)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentPadding = PaddingValues(vertical = KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        item(key = HEADER_KEY) {
            CalendarHeader(
                displayedMonth = state.selectedDate,
                onPreviousMonth = actions.onPreviousMonth,
                onNextMonth = actions.onNextMonth,
                onToday = actions.onToday,
                modifier = contentMargin,
            )
        }
        item(key = GRID_KEY) {
            MonthGrid(
                weeks = state.weeks,
                onSelectDate = actions.onSelectDate,
                modifier = Modifier.padding(horizontal = KronoSpacing.sm),
            )
        }
        dayAgenda(
            selectedDate = state.selectedDate,
            agenda = state.agenda,
            onOpenTask = actions.onOpenTask,
            onRetry = actions.onRetry,
            itemModifier = contentMargin,
        )
    }
}

private const val HEADER_KEY = "header"
private const val GRID_KEY = "month-grid"
