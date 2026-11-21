package com.krono.feature.tasks.assistant.agent

import com.krono.core.domain.assistant.agent.AgentProposal
import com.krono.core.domain.assistant.agent.BatchSaveResult
import com.krono.core.domain.task.Task

/**
 * Estado de la sección "Pídele algo al agente" del asistente de prioridades (F-28).
 *
 * @property input mensaje que el usuario está escribiendo.
 * @property status en qué paso va la petición.
 */
data class PlanningAgentUiState(
    val input: String = "",
    val status: AgentStatus = AgentStatus.Idle,
) {
    /** Hay un mensaje y no hay otra petición ni un guardado en curso. */
    val canSend: Boolean
        get() = input.isNotBlank() && status !is AgentStatus.Thinking && status !is AgentStatus.Saving
}

/** Paso de una petición al agente. */
sealed interface AgentStatus {
    /** Sin petición en curso. */
    data object Idle : AgentStatus

    /** Gemini está llamando las herramientas. */
    data object Thinking : AgentStatus

    /**
     * El agente propuso [proposal] y espera la confirmación del usuario.
     *
     * @property saveFailed el último intento de guardarla falló.
     */
    data class Proposed(val proposal: AgentProposal, val saveFailed: Boolean = false) : AgentStatus

    /** Se están guardando las tareas de [proposal], o reintentando tareas fallidas si [proposal] es `null`. */
    data class Saving(val proposal: AgentProposal? = null) : AgentStatus

    /** Se guardaron tareas con el resultado estructurado [result]. */
    data class Saved(val result: BatchSaveResult) : AgentStatus {
        /** Constructor secundario para inicializar con un conteo de tareas confirmadas. */
        constructor(count: Int) : this(
            BatchSaveResult(
                confirmedTasks = List(count) { Task(id = "task-$it", title = "Task $it") }
            )
        )

        /** Conteo total de tareas exitosamente guardadas (confirmadas o encoladas localmente). */
        val count: Int get() = result.successfulCount
    }

    /** La petición no se pudo atender por [reason]. */
    data class Failed(val reason: AgentFailure) : AgentStatus
}

/** Motivo por el que el agente no pudo proponer nada. */
enum class AgentFailure {
    /** Gemini no respondió (sin conexión, tiempo agotado o servicio deshabilitado). */
    UNAVAILABLE,

    /** Gemini respondió sin tareas ni mensaje. */
    NOT_UNDERSTOOD,

    /** El mensaje estaba vacío o era demasiado largo. */
    INVALID_REQUEST,
}
