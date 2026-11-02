package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.time.durationUntil
import com.krono.core.domain.time.plus
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Arma el plan del asistente de prioridades (F-12 CA-1): puntúa las tareas pendientes con
 * [PriorityScoring], sugiere la primera para hacer ahora y encadena las siguientes en
 * bloques consecutivos desde [now] considerando ritmo circadiano, fatiga acumulada en
 * cronómetro y calibración del modelo híbrido H1.
 *
 * Una tarea que ya no cabe antes de su entrega ([PriorityFactor.CANNOT_FINISH_BY_DUE]) se
 * considera infactible: se ordena después de todas las que sí caben, así que solo se
 * recomienda "ahora" si no hay otra, y siempre con esa advertencia.
 *
 * La duración de cada bloque sale de la estimación manual de la tarea (calibrada por H1 si
 * hay historial de subestimación); si no hay, de la estimación por historial (F-11); y si
 * tampoco hay, de [DEFAULT_ESTIMATE_MINUTES].
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
        val pending = tasks.filterNot { it.isCompleted }
        if (pending.isEmpty()) return null

        val baseDailyWorkedMinutes = tasks
            .filter { it.actualDuration > Duration.ZERO && it.dueDate?.date == now.date }
            .sumOf { it.actualDuration.inWholeMinutes }

        val initialRanked = pending
            .map { rank(it, tasks, now, baseDailyWorkedMinutes) }
            .sortedWith(planOrder)

        val scheduled = scheduleDynamic(
            pendingTasks = pending,
            history = tasks,
            now = now,
            baseWorkedMinutes = baseDailyWorkedMinutes,
        )

        return PriorityPlan(
            recommended = scheduled.first(),
            sequence = scheduled.drop(1),
            confidencePercent = confidenceOf(initialRanked),
            categoryBalance = balanceOf(scheduled),
        )
    }

    /** Puntúa una tarea en un momento y nivel de fatiga específicos; el bloque se asigna dinámicamente. */
    private fun rank(
        task: Task,
        history: List<Task>,
        at: LocalDateTime,
        dailyWorkedMinutes: Long,
    ): PlannedTask {
        val (estimate, source) = estimateFor(task, history, at)
        val timeUntilDue = task.dueDate?.let { at.durationUntil(it) }
        val result = PriorityScoring.score(
            task = task,
            timeUntilDue = timeUntilDue,
            estimate = estimate,
            hourOfDay = at.hour,
            dailyWorkedMinutes = dailyWorkedMinutes,
        )
        return PlannedTask(
            task = task,
            score = result.score,
            factors = result.factors,
            estimate = estimate,
            estimateSource = source,
            start = at,
            end = at,
            timeUntilDue = timeUntilDue,
        )
    }

    private fun estimateFor(task: Task, history: List<Task>, now: LocalDateTime): Pair<Duration, EstimateSource> {
        if (task.estimatedDuration != null && task.estimatedDuration.isPositive()) {
            val hasCalibratedHistory = history.count {
                it.isCompleted &&
                    it.category == task.category &&
                    it.actualDuration > Duration.ZERO &&
                    it.estimatedDuration?.isPositive() == true
            } >= EstimateTaskDurationUseCase.MIN_SAMPLE_SIZE

            if (hasCalibratedHistory) {
                val daysUntilDue = task.dueDate?.let {
                    (now.durationUntil(it).inWholeMinutes.toDouble() / 1440.0).coerceAtLeast(0.0)
                } ?: 2.0
                val calibrated = estimateTaskDuration.predictHybrid(
                    category = task.category,
                    tasks = history,
                    baseEstimate = task.estimatedDuration,
                    priority = task.priority,
                    daysUntilDue = daysUntilDue,
                    hourOfDay = now.hour,
                ) ?: task.estimatedDuration
                return calibrated to EstimateSource.MANUAL
            } else {
                return task.estimatedDuration to EstimateSource.MANUAL
            }
        }

        val hasEstimatedInHistory = history.any {
            it.isCompleted &&
                it.category == task.category &&
                it.actualDuration > Duration.ZERO &&
                it.estimatedDuration?.isPositive() == true
        }
        val suggested = if (hasEstimatedInHistory) {
            val daysUntilDue = task.dueDate?.let {
                (now.durationUntil(it).inWholeMinutes.toDouble() / 1440.0).coerceAtLeast(0.0)
            } ?: 2.0
            estimateTaskDuration.predictHybrid(
                category = task.category,
                tasks = history,
                priority = task.priority,
                daysUntilDue = daysUntilDue,
                hourOfDay = now.hour,
            ) ?: estimateTaskDuration(task.category, history)
        } else {
            estimateTaskDuration(task.category, history)
        }
        suggested?.let { return it to EstimateSource.HISTORY }
        return DEFAULT_ESTIMATE_MINUTES.minutes to EstimateSource.DEFAULT
    }

    /**
     * Secuenciación dinámica H2: encadena bloques seleccionando en cada paso la tarea pendiente
     * que maximiza la afinidad contextual y urgencia según la hora y la fatiga acumulada.
     */
    private fun scheduleDynamic(
        pendingTasks: List<Task>,
        history: List<Task>,
        now: LocalDateTime,
        baseWorkedMinutes: Long,
    ): List<PlannedTask> {
        val remaining = pendingTasks.toMutableList()
        val scheduled = mutableListOf<PlannedTask>()
        var cursor = now
        var currentWorkedMinutes = baseWorkedMinutes

        while (remaining.isNotEmpty() && scheduled.size < MAX_PLANNED_TASKS) {
            val rankedCandidates = remaining
                .map { rank(it, history, cursor, currentWorkedMinutes) }
                .sortedWith(planOrder)

            val chosen = rankedCandidates.first()
            val end = cursor + chosen.estimate
            val planned = chosen.copy(start = cursor, end = end)
            scheduled += planned

            remaining.removeIf { it.id == chosen.task.id }
            cursor = end
            currentWorkedMinutes += chosen.estimate.inWholeMinutes
        }

        return scheduled
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

        /** Las que caben primero; luego mayor puntuación; a igualdad, la entrega más cercana y luego la más corta. */
        private val planOrder: Comparator<PlannedTask> =
            compareBy<PlannedTask> { PriorityFactor.CANNOT_FINISH_BY_DUE in it.factors }
                .thenByDescending { it.score }
                .thenBy(nullsLast(naturalOrder<LocalDateTime>())) { it.task.dueDate }
                .thenBy { it.estimate }
                .thenBy { it.task.title }
    }
}
