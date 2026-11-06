package com.krono.core.domain.assistant.agent

import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/**
 * Datos de una tarea tal como los propone el modelo de lenguaje al llamar `create_task`
 * (F-28). Son solo los datos que dijo el usuario: la duración la estima H1 y el orden lo
 * decide H2, nunca el modelo.
 *
 * @property dueDate fecha límite que el modelo dedujo del mensaje ("el jueves"), o `null`.
 * @property userEstimate duración que el usuario dijo explícitamente, o `null`.
 */
data class TaskDraftRequest(
    val title: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val dueDate: LocalDateTime?,
    val userEstimate: Duration?,
)

/** Duración que H1 calculó para un borrador y de dónde salió. */
data class DraftEstimate(val duration: Duration, val source: EstimateSource)

/**
 * Tarea que propone el agente y que todavía no se guarda: el usuario la confirma antes (F-28).
 *
 * @property id identificador definitivo de la tarea; se reutiliza al guardar para que un
 * reintento sobrescriba en vez de duplicar.
 * @property alias identificador corto que ve el modelo ("t1", "t2"…); más fácil de repetir
 * sin errores que un UUID.
 * @property estimate duración estimada por H1, o `null` mientras no se haya estimado.
 */
data class TaskDraft(
    val id: String,
    val alias: String,
    val request: TaskDraftRequest,
    val estimate: DraftEstimate? = null,
)

/**
 * Convierte el borrador en la [Task] que se guardará.
 *
 * Se guarda como estimación la que dijo el usuario o, si no dijo ninguna, la del historial
 * (igual que cuando el usuario acepta la sugerencia de F-11 al crear una tarea). La duración
 * por defecto no se guarda: es un supuesto del plan, no una estimación.
 */
fun TaskDraft.toTask(): Task = Task(
    id = id,
    title = request.title,
    category = request.category,
    priority = request.priority,
    dueDate = request.dueDate,
    estimatedDuration = request.userEstimate
        ?: estimate?.takeIf { it.source == EstimateSource.HISTORY }?.duration,
)
