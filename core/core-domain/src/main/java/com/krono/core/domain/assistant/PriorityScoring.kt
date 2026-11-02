package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Heurística local del asistente de prioridades (F-12, D-5 opción híbrida): puntúa cada
 * tarea pendiente con datos verificables (urgencia de la entrega, prioridad asignada y
 * margen de tiempo) para que el orden sea determinista, explicable y funcione sin conexión.
 *
 * Puntuación = puntos de urgencia + puntos de prioridad + bonificación por margen ajustado
 * + adaptaciones contextuales circadiana, de fatiga y de subtareas (H2).
 * Una tarea que ya no cabe antes de su entrega recibe la advertencia
 * [PriorityFactor.CANNOT_FINISH_BY_DUE] en lugar de la bonificación por margen ajustado.
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
    fun score(task: Task, timeUntilDue: Duration?, estimate: Duration): Result =
        score(task, timeUntilDue, estimate, hourOfDay = 14, dailyWorkedMinutes = 0L)

    /**
     * Sobrecarga enriquecida que evalúa contexto circadiano, fatiga por cronómetro y subtareas (H2).
     *
     * @param hourOfDay hora del día para evaluar ritmo circadiano (0..23).
     * @param dailyWorkedMinutes minutos de trabajo acumulados en el cronómetro hoy.
     */
    fun score(
        task: Task,
        timeUntilDue: Duration?,
        estimate: Duration,
        hourOfDay: Int = 14,
        dailyWorkedMinutes: Long = 0L,
    ): Result {
        val factors = mutableSetOf<PriorityFactor>()
        dueFactor(timeUntilDue)?.let(factors::add)
        if (task.priority == TaskPriority.ALTA) factors += PriorityFactor.HIGH_PRIORITY
        when {
            cannotFinishByDue(timeUntilDue, estimate) -> factors += PriorityFactor.CANNOT_FINISH_BY_DUE
            isTight(timeUntilDue, estimate) -> factors += PriorityFactor.TIGHT_SCHEDULE
        }

        var score = urgencyPoints(factors) + priorityPoints(task.priority) +
            if (PriorityFactor.TIGHT_SCHEDULE in factors) TIGHT_SCHEDULE_POINTS else 0

        val isUrgent = PriorityFactor.OVERDUE in factors || PriorityFactor.DUE_VERY_SOON in factors

        // 1. Afinidad Circadiana y Carga Cognitiva
        val isHighCognitiveLoad = task.category == TaskCategory.ESTUDIO ||
            task.category == TaskCategory.TRABAJO ||
            task.subtasks.count { !it.isDone } >= 2

        if (hourOfDay in 6..11) {
            if (isHighCognitiveLoad && task.dueDate != null) {
                score += 2
            }
        } else if (hourOfDay >= 19 || hourOfDay < 6) {
            if (!isUrgent) {
                val isLightOrShort = estimate <= 35.minutes || task.category == TaskCategory.PERSONAL
                val isLongHeavy = estimate >= 60.minutes && (task.category == TaskCategory.ESTUDIO || task.category == TaskCategory.TRABAJO)
                if (isLightOrShort) {
                    score += 2
                } else if (isLongHeavy) {
                    score -= 1
                }
            }
        }

        // 2. Fatiga Acumulada por Cronómetro (dailyWorkedMinutes)
        if (dailyWorkedMinutes >= 90L && !isUrgent && estimate <= 30.minutes) {
            score += 1
        }

        // 3. Progreso de Subtareas (Efecto Zeigarnik / continuidad)
        if (task.subtasks.isNotEmpty() && task.subtasks.any { it.isDone } && task.subtasks.any { !it.isDone }) {
            score += 1
        }

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

    // Una tarea vencida no es "no cabe": ya cuenta como OVERDUE, que es más grave.
    private fun cannotFinishByDue(timeUntilDue: Duration?, estimate: Duration): Boolean =
        timeUntilDue != null && timeUntilDue.isPositive() && estimate > timeUntilDue

    // Una tarea vencida ya no tiene margen que cuidar: cuenta como OVERDUE, no como ajustada.
    private fun isTight(timeUntilDue: Duration?, estimate: Duration): Boolean =
        timeUntilDue != null && timeUntilDue.isPositive() && estimate >= timeUntilDue / TIGHT_MARGIN_DIVISOR
}
