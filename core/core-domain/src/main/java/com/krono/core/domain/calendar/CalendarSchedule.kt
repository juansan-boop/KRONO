package com.krono.core.domain.calendar

import com.krono.core.domain.task.Task
import kotlinx.datetime.LocalDate

/**
 * Agenda de un [CalendarPeriod]: los bloques que se solapan con el período, ya ordenados.
 *
 * El orden es por inicio ascendente y, a igual inicio, por prioridad (la más alta primero); los
 * empates restantes se resuelven por fin e identificador para que el resultado sea estable.
 *
 * @property period Período consultado.
 * @property blocks Bloques del período sin repetir, ordenados. Incluye los de tareas completadas
 * (la UI decide cómo mostrarlas, [CalendarBlock.isCompleted]).
 */
data class CalendarSchedule(
    val period: CalendarPeriod,
    val blocks: List<CalendarBlock>,
) {
    /**
     * Bloques agrupados por cada día visible de [period], incluidos los días vacíos y los de
     * meses vecinos de la rejilla. Un bloque que cruza la medianoche aparece en cada día que toca.
     */
    val blocksByDay: Map<LocalDate, List<CalendarBlock>> =
        period.days.associateWith { day -> blocks.filter { it.occursOn(day) } }

    /** Bloques del día [date] en orden (agenda de la vista diaria y de la vista mensual, F-16 CA-2). */
    fun blocksOn(date: LocalDate): List<CalendarBlock> = blocksByDay[date].orEmpty()

    /** Construcción de la agenda a partir de las tareas y orden canónico de sus bloques. */
    companion object {
        // compareByDescending usa el orden natural de TaskPriority (BAJA < MEDIA < ALTA) para
        // poner ALTA primero sin depender de la aritmética sobre ordinal.
        private val BLOCK_ORDER: Comparator<CalendarBlock> = compareBy<CalendarBlock> { it.start }
            .thenByDescending { it.priority }
            .thenBy { it.end }
            .thenBy { it.taskId }

        /**
         * Construye la agenda de [period] a partir de [tasks]: descarta las tareas sin fecha
         * límite y los bloques que no se solapan con el período (inicio inclusivo, fin exclusivo).
         */
        fun from(tasks: List<Task>, period: CalendarPeriod): CalendarSchedule {
            val periodStart = period.firstDay.startOfDay()
            val periodEnd = period.endExclusive.startOfDay()
            val blocks = tasks
                .mapNotNull { it.toCalendarBlock() }
                .filter { it.start < periodEnd && it.end > periodStart }
                .sortedWith(BLOCK_ORDER)
            return CalendarSchedule(period, blocks)
        }
    }
}
