package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.time.durationUntil
import com.krono.core.domain.time.plus
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Arma el plan del asistente de prioridades (F-12 CA-1): puntúa las tareas pendientes con
 * [PriorityScoring], sugiere la primera para hacer ahora y encadena las siguientes en
 * bloques consecutivos desde [now].
 *
 * La duración de cada bloque sale de la estimación manual de la tarea; si no hay, de la
 * estimación por historial (F-11); y si tampoco hay, de [DEFAULT_ESTIMATE_MINUTES].
 */
class BuildPriorityPlanUseCase(
    private val estimateTaskDuration: EstimateTaskDurationUseCase,
) {

    /**
     * @param tasks todas las tareas del usuario; las completadas solo se usan como historial.
     * @param now momento desde el que se planifica.
     * @return el plan, o `null` si no hay tareas pendientes.
     */
    operator fun invoke(tasks: List<Task>, now: LocalDateTime): PriorityPlan? {
        val ranked = tasks
            .filterNot { it.isCompleted }
            .map { rank(it, tasks, now) }
            .sortedWith(planOrder)
        if (ranked.isEmpty()) return null

        val scheduled = scheduleFrom(now, ranked.take(MAX_PLANNED_TASKS))
        return PriorityPlan(
            recommended = scheduled.first(),
            sequence = scheduled.drop(1),
            confidencePercent = confidenceOf(ranked),
            categoryBalance = balanceOf(scheduled),
        )
    }

    /** Puntúa una tarea; el horario se asigna después, al encadenar el plan. */
    private fun rank(task: Task, history: List<Task>, now: LocalDateTime): PlannedTask {
        val (estimate, source) = estimateFor(task, history)
        val timeUntilDue = task.dueDate?.let { now.durationUntil(it) }
        val result = PriorityScoring.score(task, timeUntilDue, estimate)
        return PlannedTask(
            task = task,
            score = result.score,
            factors = result.factors,
            estimate = estimate,
            estimateSource = source,
            start = now,
            end = now,
            timeUntilDue = timeUntilDue,
        )
    }

    private fun estimateFor(task: Task, history: List<Task>): Pair<Duration, EstimateSource> {
        task.estimatedDuration?.takeIf { it.isPositive() }?.let { return it to EstimateSource.MANUAL }
        estimateTaskDuration(task.category, history)?.let { return it to EstimateSource.HISTORY }
        return DEFAULT_ESTIMATE_MINUTES.minutes to EstimateSource.DEFAULT
    }

    private fun scheduleFrom(now: LocalDateTime, tasks: List<PlannedTask>): List<PlannedTask> {
        var cursor = now
        return tasks.map { planned ->
            val end = cursor + planned.estimate
            planned.copy(start = cursor, end = end).also { cursor = end }
        }
    }

    /**
     * Confianza en la recomendación: parte de [BASE_CONFIDENCE] y sube cuanto más se
     * distancia la primera tarea de la segunda y cuanto mejores son sus datos (estimación
     * real y fecha de entrega). Se limita a [MAX_CONFIDENCE] porque es una heurística.
     */
    private fun confidenceOf(ranked: List<PlannedTask>): Int {
        val top = ranked.first()
        val gap = (top.score - (ranked.getOrNull(1)?.score ?: 0)).coerceAtMost(MAX_SCORE_GAP)
        val estimateBonus = if (top.estimateSource != EstimateSource.DEFAULT) ESTIMATE_BONUS else 0
        val dueDateBonus = if (top.task.dueDate != null) DUE_DATE_BONUS else 0
        return (BASE_CONFIDENCE + gap * SCORE_GAP_WEIGHT + estimateBonus + dueDateBonus)
            .coerceIn(BASE_CONFIDENCE, MAX_CONFIDENCE)
    }

    /** Porcentaje del tiempo planificado por categoría; el redondeo se ajusta para sumar 100. */
    private fun balanceOf(planned: List<PlannedTask>): Map<TaskCategory, Int> {
        val minutesByCategory = TaskCategory.entries.associateWith { category ->
            planned.filter { it.task.category == category }.sumOf { it.estimate.inWholeMinutes }
        }
        val total = minutesByCategory.values.sum().coerceAtLeast(1)
        val percentages = minutesByCategory
            .mapValues { (_, minutes) -> (minutes * FULL_PERCENT.toDouble() / total).roundToInt() }
            .toMutableMap()
        val largest = minutesByCategory.maxBy { it.value }.key
        percentages[largest] = percentages.getValue(largest) + FULL_PERCENT - percentages.values.sum()
        return percentages
    }

    /** Parámetros del plan, públicos para que la UI y las pruebas no los repitan. */
    companion object {
        /** Duración que se asume para una tarea sin estimación ni historial. */
        const val DEFAULT_ESTIMATE_MINUTES = 30

        /** Tareas que entran al plan: la recomendada más la secuencia. */
        const val MAX_PLANNED_TASKS = 5

        /** Confianza mínima que se muestra. */
        const val BASE_CONFIDENCE = 50

        /** Confianza máxima que se muestra. */
        const val MAX_CONFIDENCE = 95

        private const val MAX_SCORE_GAP = 4
        private const val SCORE_GAP_WEIGHT = 8
        private const val ESTIMATE_BONUS = 10
        private const val DUE_DATE_BONUS = 5
        private const val FULL_PERCENT = 100

        /** Mayor puntuación primero; a igualdad, la entrega más cercana y luego la más corta. */
        private val planOrder: Comparator<PlannedTask> =
            compareByDescending<PlannedTask> { it.score }
                .thenBy(nullsLast(naturalOrder<LocalDateTime>())) { it.task.dueDate }
                .thenBy { it.estimate }
                .thenBy { it.task.title }
    }
}
