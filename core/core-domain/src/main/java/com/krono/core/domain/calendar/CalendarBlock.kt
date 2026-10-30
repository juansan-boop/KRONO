package com.krono.core.domain.calendar

import com.krono.core.domain.time.durationUntil
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.Duration

/** Origen de la duración de un [CalendarBlock]. */
enum class BlockDurationSource {
    /** El bloque usa la estimación de la tarea. */
    ESTIMATED,

    /** La tarea no tiene estimación válida: el bloque usa [DEFAULT_BLOCK_DURATION]. */
    DEFAULT,
}

/**
 * Bloque de calendario derivado de una tarea con fecha límite (D-14). No se almacena: se
 * calcula a partir de la tarea con [toCalendarBlock].
 *
 * El intervalo es semiabierto, `[start, end)`: el instante [end] ya no pertenece al bloque. Las
 * fechas son locales y sin zona horaria (igual que [Task.dueDate]): no hay conversión de zona.
 *
 * Un bloque que cruza la medianoche conserva su intervalo completo y se considera parte de cada
 * día que toca ([occursOn]).
 *
 * @property task Tarea de origen, con todos los datos que necesitan las vistas.
 * @property start Inicio del bloque: la fecha límite menos la duración.
 * @property end Fin del bloque: la fecha límite de la tarea.
 * @property durationSource Indica si la duración vino de la estimación o del valor por defecto.
 */
data class CalendarBlock(
    val task: Task,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val durationSource: BlockDurationSource,
) {
    /** Identificador de la tarea de origen. */
    val taskId: String get() = task.id

    /** Categoría de la tarea de origen (color del bloque). */
    val category: TaskCategory get() = task.category

    /** Prioridad de la tarea de origen (orden y reubicación de conflictos, F-17). */
    val priority: TaskPriority get() = task.priority

    /** Indica si la tarea de origen ya se completó. */
    val isCompleted: Boolean get() = task.isCompleted

    /** Duración del bloque, `end - start`. */
    val duration: Duration get() = start.durationUntil(end)

    /** Indica si el bloque toca el día [date]: `start < fin del día` y `end > inicio del día`. */
    fun occursOn(date: LocalDate): Boolean {
        val dayStart = date.startOfDay()
        val nextDayStart = date.plus(1, DateTimeUnit.DAY).startOfDay()
        return start < nextDayStart && end > dayStart
    }

    /**
     * Indica si el intervalo del bloque incluye [now]: `start <= now < end` (F-16 CA-3).
     *
     * Solo mira el intervalo: no excluye las tareas completadas. La UI muestra "En curso" solo
     * si además `!isCompleted`.
     */
    fun isInProgress(now: LocalDateTime): Boolean = now >= start && now < end
}
