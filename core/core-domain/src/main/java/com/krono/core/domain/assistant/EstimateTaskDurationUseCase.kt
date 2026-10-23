package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Estima la duración de una tarea nueva a partir del tiempo real medido en tareas
 * anteriores de la misma categoría (F-11, algoritmo propuesto en D-4).
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
    }
}
