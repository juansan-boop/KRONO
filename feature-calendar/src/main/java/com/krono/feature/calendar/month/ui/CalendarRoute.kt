package com.krono.feature.calendar.month.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krono.feature.calendar.CalendarViewModel
import com.krono.feature.calendar.ui.CalendarActions
import com.krono.feature.calendar.ui.CalendarScreen
import kotlinx.datetime.LocalDate

/**
 * Punto de entrada con estado de la pestaña Calendario (F-16 CA-1, CA-2 y CA-3). `app` la monta
 * en la ruta de la pestaña y decide a dónde llevan [onOpenTask] y [onCreateTask].
 *
 * Vive en el paquete `month.ui` solo para conservar la ruta de importación que ya usa `app`;
 * moverla exige un cambio en ese módulo.
 *
 * @param onOpenTask Abre el detalle de una tarea de la agenda por su identificador.
 * @param onCreateTask Abre la creación de una tarea con la fecha indicada prellenada.
 * @param modifier Modificador de diseño.
 * @param viewModel Instancia inyectada de [CalendarViewModel].
 */
@Composable
fun CalendarRoute(
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCreateTask: (LocalDate) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        actions = CalendarActions(
            onPrevious = viewModel::onPrevious,
            onNext = viewModel::onNext,
            onToday = viewModel::onToday,
            onSelectMode = viewModel::onSelectMode,
            onSelectDate = viewModel::onSelectDate,
            onRetry = viewModel::onRetry,
            onOpenTask = onOpenTask,
            onCreateTask = onCreateTask,
            onRescheduleTask = viewModel::onRescheduleTask,
        ),
        modifier = modifier,
    )
}
