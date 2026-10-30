package com.krono.core.domain.dashboard

import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.TimeSession
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

/**
 * Resumen del día para el dashboard de inicio (F-13 CA-1 y CA-3).
 *
 * "Hoy" son las tareas cuya fecha límite cae en el día consultado; las de otros días y las sin
 * fecha no cuentan en el avance.
 *
 * @property completedCount Tareas de hoy ya completadas.
 * @property pendingCount Tareas de hoy aún pendientes.
 * @property focusTime Tiempo trabajado hoy según las sesiones del día.
 * @property priorityTasks Pendientes de hoy, de la más a la menos prioritaria y, a igual
 * prioridad, por hora límite.
 */
data class DashboardSummary(
    val completedCount: Int,
    val pendingCount: Int,
    val focusTime: Duration,
    val priorityTasks: List<Task>,
) {
    /** Porcentaje del día completado (0 a 100, redondeado); 0 si hoy no hay tareas. */
    val completedPercent: Int
        get() {
            val total = completedCount + pendingCount
            return if (total == 0) 0 else Math.round(completedCount * PERCENT_BASE / total.toDouble()).toInt()
        }

    /** Hay al menos una tarea para hoy, completada o no. */
    val hasTasksToday: Boolean get() = completedCount + pendingCount > 0

    /** Construcción del resumen a partir de las tareas y las sesiones. */
    companion object {
        private const val PERCENT_BASE = 100

        // La prioridad alta va primero; a igual prioridad manda la hora límite más próxima.
        private val PRIORITY_ORDER: Comparator<Task> =
            compareByDescending<Task> { it.priority }.thenBy { it.dueDate }.thenBy { it.id }

        /**
         * Resume el día [today].
         *
         * @param tasks Todas las tareas del usuario; se filtran las que vencen hoy.
         * @param sessions Sesiones de tiempo del día (el repositorio ya las acota a hoy).
         * @param today Día a resumir.
         */
        fun from(tasks: List<Task>, sessions: List<TimeSession>, today: LocalDate): DashboardSummary {
            val dueToday = tasks.filter { it.dueDate?.date == today }
            val (completed, pending) = dueToday.partition { it.isCompleted }
            return DashboardSummary(
                completedCount = completed.size,
                pendingCount = pending.size,
                focusTime = sessions.fold(Duration.ZERO) { total, session -> total + session.duration },
                priorityTasks = pending.sortedWith(PRIORITY_ORDER),
            )
        }
    }
}
