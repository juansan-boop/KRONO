package com.krono.feature.dashboard

import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/** Estado de la pantalla de inicio (F-13). */
sealed interface DashboardUiState {

    /** Aún no llegaron las tareas ni las sesiones del día. */
    data object Loading : DashboardUiState

    /** No se pudieron leer las tareas o las sesiones; la pantalla ofrece reintentar. */
    data object Error : DashboardUiState

    /**
     * Resumen del día cargado.
     *
     * @property userName Nombre con el que se saluda, o `null` si el perfil aún no tiene uno.
     * @property completedPercent Porcentaje del día completado (0 a 100).
     * @property completedCount Tareas de hoy completadas.
     * @property pendingCount Tareas de hoy pendientes.
     * @property focusTime Tiempo de enfoque acumulado hoy.
     * @property hasTasksToday Hoy hay al menos una tarea; si no, la pantalla muestra el estado vacío.
     * @property priorityTasks Pendientes de hoy más prioritarias, en orden y limitadas a
     * [MAX_PRIORITY_TASKS] (el resto se ve en "Ver todas").
     */
    data class Content(
        val userName: String?,
        val completedPercent: Int,
        val completedCount: Int,
        val pendingCount: Int,
        val focusTime: Duration,
        val hasTasksToday: Boolean,
        val priorityTasks: List<PriorityTaskUi>,
    ) : DashboardUiState
}

/**
 * Tarea pendiente de hoy en la lista de prioritarias.
 *
 * @property taskId Identificador de la tarea, para abrir su detalle.
 * @property title Título visible.
 * @property dueTime Hora límite de hoy, o `null` si la fecha no trae hora.
 * @property category Categoría de la tarea.
 * @property priority Prioridad de la tarea.
 */
data class PriorityTaskUi(
    val taskId: String,
    val title: String,
    val dueTime: LocalTime?,
    val category: TaskCategory,
    val priority: TaskPriority,
)

/** Cuántas tareas prioritarias muestra el inicio antes de enviar al usuario a "Ver todas". */
const val MAX_PRIORITY_TASKS = 3
