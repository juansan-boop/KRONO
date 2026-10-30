package com.krono.feature.calendar.month.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krono.feature.calendar.CalendarViewModel
import com.krono.feature.calendar.ui.CalendarActions
import com.krono.feature.calendar.ui.CalendarScreen

/**
 * Punto de entrada con estado de la pestaña Calendario (F-16 CA-1, CA-2 y CA-3). `app` la monta
 * en la ruta de la pestaña y decide a dónde lleva [onOpenTask].
 *
 * Vive en el paquete `month.ui` solo para conservar la ruta de importación que ya usa `app`;
 * moverla exige un cambio en ese módulo.
 *
 * @param onOpenTask Abre el detalle de una tarea de la agenda por su identificador.
 */
@Composable
fun CalendarRoute(
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        actions = CalendarActions(
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onToday = viewModel::onToday,
            onSelectDate = viewModel::onSelectDate,
            onRetry = viewModel::onRetry,
            onOpenTask = onOpenTask,
        ),
        modifier = modifier,
    )
}
