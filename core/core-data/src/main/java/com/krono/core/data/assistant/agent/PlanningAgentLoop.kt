package com.krono.core.data.assistant.agent

import com.krono.core.domain.assistant.agent.PlanningAgentSession
import kotlinx.serialization.json.JsonObject

/** Respuesta de una herramienta para devolverle al modelo. */
internal data class ToolResult(val name: String, val response: JsonObject)

/** Un turno del modelo: texto para el usuario y las herramientas que pide llamar. */
internal data class AgentTurn(val text: String?, val toolCalls: List<ToolCall>)

/**
 * Conversación con el modelo, separada de Firebase para probar [PlanningAgentLoop] en la JVM.
 */
internal interface AgentConversation {

    /** Envía el mensaje inicial del usuario. */
    suspend fun send(message: String): AgentTurn

    /** Devuelve al modelo las respuestas de las herramientas que pidió. */
    suspend fun sendToolResults(results: List<ToolResult>): AgentTurn
}

/**
 * Bucle de function calling del agente (F-28): mientras el modelo pida herramientas, las
 * atiende con [dispatcher] y le devuelve los resultados; cuando responde solo con texto,
 * termina con ese texto.
 */
internal class PlanningAgentLoop(
    private val dispatcher: PlanningToolDispatcher,
) {

    /**
     * Atiende [firstMessage] sobre [session].
     *
     * @return el texto final del modelo; vacío si no redactó ninguno o si se agotaron los
     * [MAX_TOOL_ROUNDS] turnos (la propuesta se arma igual con lo que haya en la sesión).
     */
    suspend fun run(conversation: AgentConversation, firstMessage: String, session: PlanningAgentSession): String {
        var turn = conversation.send(firstMessage)
        repeat(MAX_TOOL_ROUNDS) {
            if (turn.toolCalls.isEmpty()) return turn.text.orEmpty()
            val results = turn.toolCalls.map { ToolResult(it.name, dispatcher.dispatch(it, session)) }
            turn = conversation.sendToolResults(results)
        }
        return if (turn.toolCalls.isEmpty()) turn.text.orEmpty() else ""
    }

    /** Límites del bucle. */
    companion object {
        /**
         * Rondas de herramientas por petición. Crear, estimar y planificar caben en tres; el
         * resto es margen para que el modelo corrija llamadas inválidas sin quedar en bucle.
         */
        const val MAX_TOOL_ROUNDS = 6
    }
}
