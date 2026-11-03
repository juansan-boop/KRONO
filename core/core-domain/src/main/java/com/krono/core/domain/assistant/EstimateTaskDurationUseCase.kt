package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Estima la duración de una tarea nueva a partir del tiempo real medido en tareas
 * anteriores de la misma categoría (F-11, algoritmo propuesto en D-4), e incorpora el
 * modelo híbrido H1 (regresión Ridge log-log + adaptación online al sesgo del usuario).
 *
 * Toma las últimas [MAX_SAMPLE_SIZE] tareas completadas de la categoría que tengan tiempo
 * real registrado, promedia su `actualDuration` y redondea al múltiplo de
 * [ROUNDING_STEP_MINUTES] minutos más cercano. Si hay menos de [MIN_SAMPLE_SIZE] tareas
 * válidas no hay sugerencia (CA-2).
 */
class EstimateTaskDurationUseCase {

    /**
     * Calcula la estimación para [category] usando el historial [tasks].
     *
     * @return la duración sugerida, o `null` si el historial no alcanza el mínimo.
     */
    operator fun invoke(category: TaskCategory, tasks: List<Task>): Duration? {
        val sample = latestMeasuredTasks(category, tasks)
        if (sample.size < MIN_SAMPLE_SIZE) return null
        val averageMinutes = sample.map { it.actualDuration.inWholeSeconds }.average() / SECONDS_PER_MINUTE
        return roundToStep(averageMinutes)
    }

    /**
     * Elemento de Aprendizaje H1: Predice la duración combinando la regresión lineal
     * log-log (M1) entrenada con los parámetros de `modelo_app.json` y la corrección online
     * al sesgo de estimación observado en las tareas recientes del usuario.
     *
     * @param category Categoría de la tarea a estimar.
     * @param tasks Historial de tareas del usuario.
     * @param baseEstimate Estimación subjetiva o manual previa, si existe.
     * @param priority Prioridad de la tarea (Baja = 1, Media = 2, Alta = 3).
     * @param daysUntilDue Días restantes hasta la fecha límite.
     * @param hourOfDay Hora del día prevista para inicio/ejecución (0 a 23).
     * @return Duración predicha y redondeada a múltiplos de [ROUNDING_STEP_MINUTES], o `null` si no hay datos.
     */
    fun predictHybrid(
        category: TaskCategory,
        tasks: List<Task>,
        baseEstimate: Duration? = null,
        priority: TaskPriority = TaskPriority.MEDIA,
        daysUntilDue: Double = 2.0,
        hourOfDay: Int = 14,
    ): Duration? {
        val estMinutes = if (baseEstimate != null && baseEstimate.isPositive()) {
            baseEstimate.inWholeMinutes.toDouble().coerceAtLeast(5.0)
        } else {
            val hist = invoke(category, tasks) ?: return null
            hist.inWholeMinutes.toDouble().coerceAtLeast(5.0)
        }

        val predM1 = computeM1(estMinutes, priority, daysUntilDue, hourOfDay)

        // Adaptación online: calcula la razón duracion_real / pred_m1 en tareas recientes
        val recentMeasured = tasks
            .filter { it.isCompleted && !it.isDeleted && it.category == category && it.actualDuration > Duration.ZERO }
            .sortedWith(compareByDescending(nullsFirst(naturalOrder<LocalDateTime>())) { it.dueDate })
            .take(HYBRID_WINDOW)

        val ratios = recentMeasured.mapNotNull { pastTask ->
            val pastEst = pastTask.estimatedDuration?.takeIf { it.isPositive() }?.inWholeMinutes?.toDouble()
            if (pastEst != null) {
                val pastHour = pastTask.dueDate?.hour ?: 14
                val pastM1 = computeM1(pastEst, pastTask.priority, 2.0, pastHour)
                pastTask.actualDuration.inWholeMinutes.toDouble().coerceAtLeast(1.0) / pastM1
            } else {
                null
            }
        }

        val factor = if (ratios.isNotEmpty()) {
            val n = ratios.size.toDouble()
            val meanLog = ratios.map { ln(it) }.average()
            val personalBias = exp(meanLog)
            val weight = n / (n + HYBRID_K)
            1.0 + (personalBias - 1.0) * weight
        } else {
            1.0
        }

        val finalMinutes = predM1 * factor
        return roundToStep(finalMinutes)
    }

    private fun computeM1(
        estMinutes: Double,
        priority: TaskPriority,
        daysUntilDue: Double,
        hourOfDay: Int,
    ): Double {
        val blockCoef = when {
            hourOfDay < 12 -> COEF_BLOCK_MORNING
            hourOfDay < 18 -> COEF_BLOCK_AFTERNOON
            else -> COEF_BLOCK_NIGHT
        }
        val priorityValue = when (priority) {
            TaskPriority.BAJA -> 1.0
            TaskPriority.MEDIA -> 2.0
            TaskPriority.ALTA -> 3.0
        }
        val logPred = INTERCEPT +
            COEF_LOG_ESTIMATE * ln(estMinutes.coerceAtLeast(5.0)) +
            COEF_DAYS_TO_DUE * daysUntilDue.coerceAtLeast(0.0) +
            COEF_PRIORITY * priorityValue +
            blockCoef
        return exp(logPred).coerceAtLeast(5.0)
    }

    /**
     * Tareas completadas de la categoría con tiempo real medido, de la más reciente a la
     * más antigua.
     *
     * `Task` no guarda la fecha en que se completó, así que la fecha de entrega se usa como
     * aproximación de "más reciente"; las tareas sin fecha quedan al final.
     */
    private fun latestMeasuredTasks(category: TaskCategory, tasks: List<Task>): List<Task> =
        tasks
            .filter { it.isCompleted && it.category == category && it.actualDuration > Duration.ZERO }
            .sortedWith(compareByDescending(nullsFirst(naturalOrder<LocalDateTime>())) { it.dueDate })
            .take(MAX_SAMPLE_SIZE)

    private fun roundToStep(minutes: Double): Duration {
        val steps = (minutes / ROUNDING_STEP_MINUTES).roundToInt().coerceAtLeast(1)
        return (steps * ROUNDING_STEP_MINUTES).minutes
    }

    /** Parámetros del algoritmo, públicos para que la UI y las pruebas no los repitan. */
    companion object {
        /** Mínimo de tareas medidas para ofrecer una sugerencia (CA-1, CA-2). */
        const val MIN_SAMPLE_SIZE = 3

        /** Cantidad de tareas recientes que entran al promedio. */
        const val MAX_SAMPLE_SIZE = 5

        /** Granularidad del redondeo de la estimación. */
        const val ROUNDING_STEP_MINUTES = 5

        private const val SECONDS_PER_MINUTE = 60.0

        // Constantes del Modelo H1 exportadas de modelo_app.json
        const val INTERCEPT = 0.748545
        const val COEF_LOG_ESTIMATE = 0.872979
        const val COEF_DAYS_TO_DUE = -0.000405
        const val COEF_PRIORITY = -0.000726
        const val COEF_BLOCK_MORNING = -0.150017
        const val COEF_BLOCK_AFTERNOON = -0.038374
        const val COEF_BLOCK_NIGHT = 0.188390
        const val GLOBAL_FACTOR_B1 = 1.28
        const val HYBRID_K = 2.0
        const val HYBRID_WINDOW = 15
    }
}
