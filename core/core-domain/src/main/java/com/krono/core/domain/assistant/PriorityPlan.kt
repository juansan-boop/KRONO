package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/**
 * Motivos por los que el asistente de prioridades (F-12) adelanta una tarea. Son datos
 * verificables de la tarea, la base de la explicación que se muestra al usuario.
 */
enum class PriorityFactor {
    /**
     * Advertencia: la entrega aún no vence, pero la estimación es mayor que el tiempo que
     * queda, así que ya no alcanza a terminarla a tiempo si empieza ahora. Va primero para
     * que la explicación de plantilla la diga antes que los demás motivos.
     */
    CANNOT_FINISH_BY_DUE,

    /** La fecha de entrega ya pasó. */
    OVERDUE,

    /** Vence en pocas horas (ver [PriorityScoring.VERY_SOON_HOURS]). */
    DUE_VERY_SOON,

    /** Vence en las próximas 24 horas. */
    DUE_TODAY,

    /** Vence en los próximos días (ver [PriorityScoring.SOON_HOURS]). */
    DUE_SOON,

    /** El usuario le asignó prioridad alta. */
    HIGH_PRIORITY,

    /** El tiempo estimado ocupa casi todo el margen que queda antes de la entrega. */
    TIGHT_SCHEDULE,
}

/** Origen de la duración con que se planifica una tarea. */
enum class EstimateSource {
    /** Estimación que el usuario puso en la tarea. */
    MANUAL,

    /** Estimación calculada con el historial de la categoría (F-11). */
    HISTORY,

    /** Sin datos: se usa la duración por defecto del asistente. */
    DEFAULT,
}

/**
 * Una tarea ubicada en el plan del asistente.
 *
 * @property score puntuación de la heurística; mayor significa más urgente.
 * @property factors motivos que explican la puntuación.
 * @property estimate duración con que se reserva el bloque.
 * @property start inicio sugerido del bloque.
 * @property end fin sugerido del bloque.
 * @property timeUntilDue tiempo hasta la entrega desde el momento del cálculo; negativo si
 * ya venció y `null` si la tarea no tiene fecha.
 */
data class PlannedTask(
    val task: Task,
    val score: Int,
    val factors: Set<PriorityFactor>,
    val estimate: Duration,
    val estimateSource: EstimateSource,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val timeUntilDue: Duration?,
)

/**
 * Resultado del asistente de prioridades (F-12 CA-1): la tarea a hacer ahora y la
 * secuencia sugerida para las siguientes.
 *
 * @property recommended tarea sugerida para empezar ya.
 * @property sequence tareas siguientes, en el orden en que conviene hacerlas.
 * @property confidencePercent qué tan clara es la recomendación (ver [PriorityScoring]);
 * depende de la diferencia de puntuación con la segunda tarea y de la calidad de los datos.
 * @property categoryBalance porcentaje del tiempo planificado por categoría; suma 100.
 */
data class PriorityPlan(
    val recommended: PlannedTask,
    val sequence: List<PlannedTask>,
    val confidencePercent: Int,
    val categoryBalance: Map<TaskCategory, Int>,
)
