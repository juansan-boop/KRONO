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
 * @property dailyFocusPercentage Porcentaje de avance de las tareas de hoy (0 a 100).
 * @property dailyPendingCount Cantidad de tareas de hoy pendientes de resolver.
 * @property todayTasks Lista de tareas clasificadas para la sección "Para Hoy".
 * @property thisWeekTasks Lista de tareas clasificadas para la sección "Esta Semana".
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
    val taskPendingDelete: Task? = null,
    val errorMessage: String? = null,
    val hasLoadError: Boolean = false,
) {
    /** Indica si no hay ninguna tarea para mostrar en la vista actual (CA-4). */
    val isEmpty: Boolean get() = todayTasks.isEmpty() && thisWeekTasks.isEmpty()

    /** Indica si corresponde el estado vacío: no hay tareas y la carga no falló (CA-4). */
    val showEmptyState: Boolean get() = !hasLoadError && isEmpty
}
