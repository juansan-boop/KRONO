package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskPriority
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Heurística local del asistente de prioridades (F-12, D-5 opción híbrida): puntúa cada
 * tarea pendiente con datos verificables (urgencia de la entrega, prioridad asignada y
 * margen de tiempo) para que el orden sea determinista, explicable y funcione sin conexión.
 *
 * Puntuación = puntos de urgencia + puntos de prioridad + bonificación por margen ajustado.
 * La urgencia pesa más que la prioridad para reducir entregas extemporáneas, una de las
 * métricas de éxito del producto.
 */
object PriorityScoring {

    /** Horas hasta la entrega por debajo de las cuales la tarea "vence en pocas horas". */
    const val VERY_SOON_HOURS = 6

    /** Horas hasta la entrega que cuentan como "vence hoy". */
    const val TODAY_HOURS = 24

    /** Horas hasta la entrega que cuentan como "vence pronto". */
    const val SOON_HOURS = 72

    private const val OVERDUE_POINTS = 8
    private const val VERY_SOON_POINTS = 6
    private const val TODAY_POINTS = 4
    private const val SOON_POINTS = 2
    private const val HIGH_PRIORITY_POINTS = 3
    private const val MEDIUM_PRIORITY_POINTS = 2
    private const val LOW_PRIORITY_POINTS = 1
    private const val TIGHT_SCHEDULE_POINTS = 2

    /**
     * Divisor del margen: si la estimación ocupa al menos la mitad del tiempo que queda,
     * el margen se considera ajustado.
     */
    private const val TIGHT_MARGIN_DIVISOR = 2

    /** Puntuación de una tarea y los motivos que la explican. */
    data class Result(val score: Int, val factors: Set<PriorityFactor>)

    /**
     * Puntúa [task] dado el tiempo que falta para su entrega y su duración estimada.
     *
     * @param timeUntilDue tiempo hasta la entrega (negativo si venció) o `null` sin fecha.
     */
    fun score(task: Task, timeUntilDue: Duration?, estimate: Duration): Result {
        val factors = mutableSetOf<PriorityFactor>()
        dueFactor(timeUntilDue)?.let(factors::add)
        if (task.priority == TaskPriority.ALTA) factors += PriorityFactor.HIGH_PRIORITY
        if (isTight(timeUntilDue, estimate)) factors += PriorityFactor.TIGHT_SCHEDULE

        val score = urgencyPoints(factors) + priorityPoints(task.priority) +
            if (PriorityFactor.TIGHT_SCHEDULE in factors) TIGHT_SCHEDULE_POINTS else 0
        return Result(score, factors)
    }

    private fun dueFactor(timeUntilDue: Duration?): PriorityFactor? = when {
        timeUntilDue == null -> null
        timeUntilDue.isNegative() -> PriorityFactor.OVERDUE
        timeUntilDue <= VERY_SOON_HOURS.hours -> PriorityFactor.DUE_VERY_SOON
        timeUntilDue <= TODAY_HOURS.hours -> PriorityFactor.DUE_TODAY
        timeUntilDue <= SOON_HOURS.hours -> PriorityFactor.DUE_SOON
        else -> null
    }

    private fun urgencyPoints(factors: Set<PriorityFactor>): Int = when {
        PriorityFactor.OVERDUE in factors -> OVERDUE_POINTS
        PriorityFactor.DUE_VERY_SOON in factors -> VERY_SOON_POINTS
        PriorityFactor.DUE_TODAY in factors -> TODAY_POINTS
        PriorityFactor.DUE_SOON in factors -> SOON_POINTS
        else -> 0
    }

    private fun priorityPoints(priority: TaskPriority): Int = when (priority) {
        TaskPriority.ALTA -> HIGH_PRIORITY_POINTS
        TaskPriority.MEDIA -> MEDIUM_PRIORITY_POINTS
        TaskPriority.BAJA -> LOW_PRIORITY_POINTS
    }

    // Una tarea vencida ya no tiene margen que cuidar: cuenta como OVERDUE, no como ajustada.
    private fun isTight(timeUntilDue: Duration?, estimate: Duration): Boolean =
        timeUntilDue != null && timeUntilDue.isPositive() && estimate >= timeUntilDue / TIGHT_MARGIN_DIVISOR
}
