package com.krono.core.domain.assistant.agent

/**
 * Errores del agente de planificación (F-28). Los de los borradores se devuelven al modelo
 * como respuesta de la herramienta para que corrija la llamada; los demás llegan a la UI.
 */
sealed class PlanningAgentError(message: String) : Exception(message) {

    /** El mensaje del usuario está vacío o es demasiado largo. */
    data class InvalidRequest(val reason: String) : PlanningAgentError(reason)

    /** El modelo pidió crear una tarea con datos que no sirven. */
    data class InvalidDraft(val reason: String) : PlanningAgentError(reason)

    /** El modelo nombró un borrador que no existe. */
    data class DraftNotFound(val alias: String) :
        PlanningAgentError("No existe una tarea propuesta con id \"$alias\".")

    /** El modelo intentó proponer más tareas de las permitidas en una sola petición. */
    data object TooManyDrafts :
        PlanningAgentError("Se alcanzó el máximo de ${PlanningAgentSession.MAX_DRAFTS} tareas por petición.")

    /** El modelo respondió sin proponer tareas ni redactar un mensaje. */
    data object NothingProposed : PlanningAgentError("El agente no propuso nada para ese mensaje.")
}
