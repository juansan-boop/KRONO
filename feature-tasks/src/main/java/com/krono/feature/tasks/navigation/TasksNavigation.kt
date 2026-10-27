package com.krono.feature.tasks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.krono.feature.tasks.assistant.ui.PriorityAssistantRoute
import com.krono.feature.tasks.create.ui.CreateTaskRoute
import com.krono.feature.tasks.detail.TaskDetailViewModel.Companion.TASK_ID_NAV_ARG
import com.krono.feature.tasks.detail.ui.TaskDetailRoute
import com.krono.feature.tasks.edit.EditTaskViewModel.Companion.TASK_ID_NAV_ARG as EDIT_TASK_ID_NAV_ARG
import com.krono.feature.tasks.edit.ui.EditTaskRoute
import com.krono.feature.tasks.ui.TasksRoute

/**
 * Ruta canónica de la pantalla de tareas en el grafo de navegación.
 */
const val TASKS_ROUTE = "tareas"

/**
 * Ruta canónica de la pantalla de creación de tarea (F-04).
 */
const val CREATE_TASK_ROUTE = "tareas/nueva"

/**
 * Ruta base para la pantalla de detalle de tarea y subtareas (F-06).
 */
const val TASK_DETAIL_ROUTE_BASE = "tareas/detalle"

/**
 * Ruta canónica con argumento de la pantalla de detalle de tarea (F-06).
 */
const val TASK_DETAIL_ROUTE = "$TASK_DETAIL_ROUTE_BASE/{$TASK_ID_NAV_ARG}"

/**
 * Ruta base para la pantalla de edición de tarea (F-05).
 */
const val EDIT_TASK_ROUTE_BASE = "tareas/editar"

/**
 * Ruta canónica con argumento de la pantalla de edición de tarea (F-05).
 */
const val EDIT_TASK_ROUTE = "$EDIT_TASK_ROUTE_BASE/{$EDIT_TASK_ID_NAV_ARG}"

/**
 * Ruta canónica del asistente de prioridades (F-12).
 */
const val PRIORITY_ASSISTANT_ROUTE = "tareas/asistente"

/**
 * Navega hacia la pantalla de tareas.
 */
fun NavController.navigateToTasks(navOptions: NavOptions? = null) {
    navigate(TASKS_ROUTE, navOptions)
}

/**
 * Navega hacia la pantalla de creación de tarea (F-04).
 */
fun NavController.navigateToCreateTask(navOptions: NavOptions? = null) {
    navigate(CREATE_TASK_ROUTE, navOptions)
}

/**
 * Navega hacia la pantalla de detalle de tarea recibiendo su [taskId] (F-06 CA-1).
 */
fun NavController.navigateToTaskDetail(taskId: String, navOptions: NavOptions? = null) {
    navigate("$TASK_DETAIL_ROUTE_BASE/$taskId", navOptions)
}

/**
 * Navega hacia la pantalla de edición de tarea recibiendo su [taskId] (F-05 CA-1).
 */
fun NavController.navigateToEditTask(taskId: String, navOptions: NavOptions? = null) {
    navigate("$EDIT_TASK_ROUTE_BASE/$taskId", navOptions)
}

/**
 * Navega hacia el asistente de prioridades (F-12).
 */
fun NavController.navigateToPriorityAssistant(navOptions: NavOptions? = null) {
    navigate(PRIORITY_ASSISTANT_ROUTE, navOptions)
}

/**
 * Registra el destino del asistente de prioridades (F-12) en el grafo de navegación.
 *
 * @param onBack Callback invocado para regresar a la pantalla anterior.
 * @param onOpenTask Callback invocado para abrir el detalle de una tarea del plan (F-06).
 */
fun NavGraphBuilder.priorityAssistantScreen(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
    composable(route = PRIORITY_ASSISTANT_ROUTE) {
        PriorityAssistantRoute(
            onBack = onBack,
            onOpenTask = onOpenTask,
        )
    }
}

/**
 * Registra el destino de la lista de tareas (F-03) en el grafo de navegación.
 *
 * @param onAddTask Callback invocado al pulsar el botón de crear nueva tarea (F-04).
 * @param onTaskClick Callback opcional al pulsar sobre una tarjeta de tarea.
 * @param onOpenAssistant Callback opcional para abrir el asistente de prioridades (F-12).
 */
fun NavGraphBuilder.tasksScreen(
    onAddTask: () -> Unit,
    onTaskClick: ((String) -> Unit)? = null,
    onOpenAssistant: (() -> Unit)? = null,
) {
    composable(route = TASKS_ROUTE) {
        TasksRoute(
            onAddTask = onAddTask,
            onTaskClick = onTaskClick,
            onOpenAssistant = onOpenAssistant,
        )
    }
}

/**
 * Registra el destino del detalle de tarea y subtareas (F-06) en el grafo de navegación.
 *
 * @param onBack Callback invocado para regresar a la lista de tareas.
 * @param onEditTask Callback invocado para editar la tarea (F-05).
 * @param onStartFocus Callback invocado para iniciar el modo enfoque con la tarea (F-09).
 */
fun NavGraphBuilder.taskDetailScreen(
    onBack: () -> Unit,
    onEditTask: (String) -> Unit,
    onStartFocus: (String) -> Unit,
) {
    composable(
        route = TASK_DETAIL_ROUTE,
        arguments = listOf(
            navArgument(TASK_ID_NAV_ARG) { type = NavType.StringType },
        ),
    ) {
        TaskDetailRoute(
            onBack = onBack,
            onEditTask = onEditTask,
            onStartFocus = onStartFocus,
        )
    }
}

/**
 * Registra el destino del formulario de creación de tarea (F-04) en el grafo de navegación.
 *
 * @param onBack Callback invocado para regresar tras guardar o cancelar.
 */
fun NavGraphBuilder.createTaskScreen(
    onBack: () -> Unit,
) {
    composable(route = CREATE_TASK_ROUTE) {
        CreateTaskRoute(
            onBack = onBack,
        )
    }
}

/**
 * Registra el destino del formulario de edición de tarea (F-05) en el grafo de navegación.
 *
 * @param onBack Callback invocado para regresar tras guardar cambios o cancelar.
 */
fun NavGraphBuilder.editTaskScreen(
    onBack: () -> Unit,
) {
    composable(
        route = EDIT_TASK_ROUTE,
        arguments = listOf(
            navArgument(EDIT_TASK_ID_NAV_ARG) { type = NavType.StringType },
        ),
    ) {
        EditTaskRoute(
            onBack = onBack,
        )
    }
}

