package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.domain.task.Task
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes

/**
 * Herramientas que el modelo de lenguaje puede llamar durante una petición al agente (F-28),
 * con el estado de esa petición: los borradores de tareas que lleva propuestos.
 *
 * - [createTask]: agrega un borrador con los datos que dijo el usuario.
 * - [estimateTask]: estima un borrador con H1 (la misma regla de [BuildPriorityPlanUseCase]).
 * - [buildPlan]: arma el plan H2 con las tareas actuales más los borradores.
 *
 * Nada se guarda aquí: el usuario confirma la [AgentProposal] antes de crear las tareas.
 *
 * @param currentTasks todas las tareas del usuario (las completadas sirven de historial a H1).
 * @param now momento desde el que se planifica.
 * @param newTaskId genera el identificador definitivo de cada borrador.
 */
class PlanningAgentSession(
    private val currentTasks: List<Task>,
    val now: LocalDateTime,
    private val buildPriorityPlan: BuildPriorityPlanUseCase,
    private val newTaskId: () -> String,
) {
    private val draftsByAlias = linkedMapOf<String, TaskDraft>()

    /** Borradores propuestos hasta ahora, en el orden en que el modelo los creó. */
    val drafts: List<TaskDraft> get() = draftsByAlias.values.toList()

    /**
     * Agrega un borrador. Si ya hay uno con el mismo título lo devuelve sin duplicarlo, porque
     * el modelo a veces repite una llamada.
     *
     * @return el borrador, o [PlanningAgentError] si los datos no sirven o se llegó al máximo.
     */
    fun createTask(request: TaskDraftRequest): KronoResult<TaskDraft> {
        invalidReason(request)?.let { return KronoResult.Error(PlanningAgentError.InvalidDraft(it)) }
        duplicateOf(request)?.let { return KronoResult.Success(it) }
        if (draftsByAlias.size >= MAX_DRAFTS) return KronoResult.Error(PlanningAgentError.TooManyDrafts)

        val alias = "$ALIAS_PREFIX${draftsByAlias.size + 1}"
        val draft = TaskDraft(id = newTaskId(), alias = alias, request = request.copy(title = request.title.trim()))
        draftsByAlias[alias] = draft
        return KronoResult.Success(draft)
    }

    /**
     * Estima con H1 el borrador [alias] y guarda la estimación en él.
     *
     * @return el borrador estimado, o [PlanningAgentError.DraftNotFound].
     */
    fun estimateTask(alias: String): KronoResult<TaskDraft> {
        val draft = draftsByAlias[alias] ?: return KronoResult.Error(PlanningAgentError.DraftNotFound(alias))
        val estimated = draft.copy(estimate = estimateOf(draft))
        draftsByAlias[alias] = estimated
        return KronoResult.Success(estimated)
    }

    /**
     * Plan H2 con las tareas actuales más los borradores (que se estiman si faltaba). El orden
     * sale de la heurística; el modelo solo lo recibe para contarlo.
     */
    fun buildPlan(): PriorityPlan? = planWith(estimateAll())

    /** Cierra la petición: estima lo que falte y arma la propuesta con el [message] del modelo. */
    fun toProposal(message: String?): AgentProposal {
        val estimated = estimateAll()
        return AgentProposal(
            drafts = estimated,
            plan = planWith(estimated),
            message = message?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun planWith(drafts: List<TaskDraft>): PriorityPlan? =
        buildPriorityPlan(currentTasks + drafts.map { it.toTask() }, now)

    // H1 garantiza la estimación de todos los borradores aunque el modelo no haya llamado
    // estimate_task: la duración nunca depende de que el modelo se acuerde.
    private fun estimateAll(): List<TaskDraft> {
        draftsByAlias.replaceAll { _, draft -> if (draft.estimate != null) draft else draft.copy(estimate = estimateOf(draft)) }
        return drafts
    }

    private fun estimateOf(draft: TaskDraft): DraftEstimate {
        // Se estima sobre la tarea sin estimación previa de H1, solo con la del usuario.
        val (duration, source) = buildPriorityPlan.estimateFor(draft.copy(estimate = null).toTask(), currentTasks, now)
        return DraftEstimate(duration, source)
    }

    private fun duplicateOf(request: TaskDraftRequest): TaskDraft? =
        draftsByAlias.values.firstOrNull { it.request.title.equals(request.title.trim(), ignoreCase = true) }

    private fun invalidReason(request: TaskDraftRequest): String? {
        val title = request.title.trim()
        val estimate = request.userEstimate
        return when {
            title.isEmpty() -> "El título no puede estar vacío."
            title.length > MAX_TITLE_LENGTH -> "El título supera los $MAX_TITLE_LENGTH caracteres."
            request.dueDate != null && request.dueDate < now ->
                "La fecha límite ya pasó; usa una fecha posterior a la actual."
            estimate != null && estimate !in MIN_USER_ESTIMATE_MINUTES.minutes..MAX_USER_ESTIMATE_MINUTES.minutes ->
                "La duración debe estar entre $MIN_USER_ESTIMATE_MINUTES y $MAX_USER_ESTIMATE_MINUTES minutos."
            else -> null
        }
    }

    /** Límites de una petición al agente. */
    companion object {
        /** Máximo de tareas que el agente puede proponer en una sola petición. */
        const val MAX_DRAFTS = 8

        /** Longitud máxima del título de una tarea propuesta. */
        const val MAX_TITLE_LENGTH = 80

        /** Duración mínima aceptada cuando el usuario dice cuánto tarda una tarea. */
        const val MIN_USER_ESTIMATE_MINUTES = 5

        /** Duración máxima aceptada cuando el usuario dice cuánto tarda una tarea. */
        const val MAX_USER_ESTIMATE_MINUTES = 480

        private const val ALIAS_PREFIX = "t"
    }
}
