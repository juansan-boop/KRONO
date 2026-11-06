package com.krono.feature.tasks.assistant.agent

import com.krono.core.domain.assistant.agent.AgentProposal

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

    /** Se están guardando las tareas de [proposal]. */
    data class Saving(val proposal: AgentProposal) : AgentStatus

    /** Se crearon [count] tareas. */
    data class Saved(val count: Int) : AgentStatus

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
