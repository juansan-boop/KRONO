package com.krono.feature.tasks

import com.krono.core.domain.task.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Estado inmutable de la pantalla de lista de tareas (F-03).
 *
 * @property isLoading Indica si los datos iniciales se están cargando.
 * @property today Fecha de referencia para clasificar hoy vs esta semana.
 * @property selectedCategory Filtro de categoría activo actualmente.
 * @property categoryCounts Conteo de tareas por cada filtro disponible.
 * @property dailyFocusPercentage Porcentaje de avance del día (0 a 100). Es el mismo cálculo del
 * resumen de Inicio ([com.krono.core.domain.dashboard.DashboardSummary]): solo cuentan las tareas
 * cuya fecha límite es hoy, de todas las categorías (no depende de [selectedCategory]).
 * @property dailyPendingCount Tareas con fecha límite hoy aún pendientes (mismo cálculo que Inicio).
 * @property todayTasks Sección "Para Hoy": las que vencen hoy (hechas o no) y las vencidas pendientes.
 * @property thisWeekTasks Sección "Esta Semana": las de días posteriores a hoy y las sin fecha.
 * @property completedEarlierTasks Sección "Completadas": las completadas con fecha límite de días
 * anteriores; son historial y no cuentan en "Para Hoy" ni en el avance del día.
 * @property taskPendingDelete Tarea seleccionada pendiente de confirmación de eliminación o posposición (F-07).
 * @property errorMessage Mensaje de error a desplegar si alguna operación falla.
 * @property hasLoadError Indica que no se pudieron leer las tareas (por ejemplo, permiso denegado);
 * la pantalla ofrece reintentar en vez de mostrar un estado vacío falso. Sin conexión no es un
 * error: la caché de Firestore sigue sirviendo las tareas.
 */
data class TasksUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val selectedCategory: TaskCategoryFilter = TaskCategoryFilter.TODAS,
    val categoryCounts: Map<TaskCategoryFilter, Int> = emptyMap(),
    val dailyFocusPercentage: Int = 0,
    val dailyPendingCount: Int = 0,
    val todayTasks: List<Task> = emptyList(),
    val thisWeekTasks: List<Task> = emptyList(),
    val completedEarlierTasks: List<Task> = emptyList(),
    val taskPendingDelete: Task? = null,
    val errorMessage: String? = null,
    val hasLoadError: Boolean = false,
) {
    /** Indica si no hay ninguna tarea para mostrar en la vista actual (CA-4). */
    val isEmpty: Boolean get() = todayTasks.isEmpty() && thisWeekTasks.isEmpty() && completedEarlierTasks.isEmpty()

    /** Indica si corresponde el estado vacío: no hay tareas y la carga no falló (CA-4). */
    val showEmptyState: Boolean get() = !hasLoadError && isEmpty
}
