package com.krono.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoSpacing
import com.krono.feature.dashboard.DashboardUiState
import com.krono.feature.dashboard.DashboardViewModel

/**
 * Punto de entrada con estado de la pestaña Inicio (F-13). `app` la monta en la ruta de la
 * pestaña y decide a dónde llevan las acciones de navegación.
 *
 * @param onStartFocus Se invoca al tocar "Iniciar" con el identificador de la tarea pendiente más
 * prioritaria de hoy, o `null` si no hay ninguna (F-13 CA-2).
 * @param onSeeAll Abre la lista completa de tareas.
 * @param onOpenTask Abre el detalle de una tarea por su identificador.
 */
@Composable
fun DashboardRoute(
    onStartFocus: (taskId: String?) -> Unit,
    onSeeAll: () -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        actions = DashboardActions(
            onStartFocus = onStartFocus,
            onSeeAll = onSeeAll,
            onOpenTask = onOpenTask,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/**
 * Acciones del inicio, agrupadas para no alargar la firma de la pantalla.
 *
 * @property onStartFocus Arranca el modo enfoque; recibe la tarea prioritaria de hoy, si la hay.
 * @property onSeeAll Abre la lista completa de tareas.
 * @property onOpenTask Abre el detalle de una tarea.
 * @property onRetry Vuelve a leer los datos tras un error.
 */
data class DashboardActions(
    val onStartFocus: (taskId: String?) -> Unit = {},
    val onSeeAll: () -> Unit = {},
    val onOpenTask: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Pantalla de inicio sin estado, según `dashboard_inicio_accesible_1`: saludo, resumen de hoy,
 * sesión de enfoque y tareas prioritarias. Va en una sola lista desplazable para que nada se
 * recorte con la fuente al 200 %.
 *
 * @param state Estado de la pantalla (carga, error o contenido).
 * @param actions Acciones del usuario.
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentPadding = PaddingValues(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        when (state) {
            DashboardUiState.Loading -> item(key = STATUS_KEY) { DashboardLoading() }
            DashboardUiState.Error -> item(key = STATUS_KEY) { DashboardErrorCard(onRetry = actions.onRetry) }
            is DashboardUiState.Content -> content(state, actions)
        }
    }
}

private fun LazyListScope.content(
    state: DashboardUiState.Content,
    actions: DashboardActions,
) {
    item(key = HEADER_KEY) { DashboardHeader(userName = state.userName) }
    item(key = SUMMARY_KEY) { DaySummaryCard(content = state) }
    item(key = FOCUS_KEY) {
        FocusSessionCard(onStart = { actions.onStartFocus(state.priorityTasks.firstOrNull()?.taskId) })
    }
    item(key = PRIORITY_KEY) {
        PriorityTasksSection(content = state, onSeeAll = actions.onSeeAll, onOpenTask = actions.onOpenTask)
    }
}

private const val STATUS_KEY = "status"
private const val HEADER_KEY = "header"
private const val SUMMARY_KEY = "summary"
private const val FOCUS_KEY = "focus"
private const val PRIORITY_KEY = "priority"
