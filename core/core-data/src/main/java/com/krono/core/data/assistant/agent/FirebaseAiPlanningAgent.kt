package com.krono.core.data.assistant.agent

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.krono.core.common.KronoResult
import com.krono.core.data.assistant.FirebaseAiPriorityExplanationGenerator
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.agent.PlanningAgent
import com.krono.core.domain.assistant.agent.PlanningAgentSession
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * Agente de planificación con Gemini y function calling de Firebase AI Logic (F-28).
 *
 * Gemini lee el mensaje del usuario y llama las herramientas de [PlanningToolDeclarations];
 * la app las ejecuta con la [PlanningAgentSession] (H1 estima y H2 ordena) y le devuelve los
 * resultados hasta que el modelo responde con texto. Usa el mismo modelo que la explicación
 * del asistente (F-12) y, como ella, no guarda claves en la app.
 */
class FirebaseAiPlanningAgent @Inject constructor() : PlanningAgent {

    private val loop = PlanningAgentLoop(PlanningToolDispatcher())

    // Perezoso: crear el modelo exige Firebase inicializado, y así las pruebas JVM no lo tocan.
    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = FirebaseAiPriorityExplanationGenerator.MODEL_NAME,
            generationConfig = generationConfig {
                temperature = TEMPERATURE
                thinkingConfig = thinkingConfig { thinkingLevel = THINKING_LEVEL }
            },
            tools = listOf(Tool.functionDeclarations(PlanningToolDeclarations.all)),
            systemInstruction = content { text(PlanningAgentPrompt.SYSTEM_INSTRUCTION) },
        )
    }

    /** Atiende [request] con Gemini; cada petición abre una conversación nueva. */
    override suspend fun run(request: String, session: PlanningAgentSession): KronoResult<String> =
        runWith(request, session) { FirebaseAgentConversation(model.startChat()) }

    /**
     * Ejecuta el bucle sobre la conversación de [openConversation] dentro del tiempo límite.
     * Separado de [run] para probar el límite y los fallos sin Firebase.
     */
    internal suspend fun runWith(
        request: String,
        session: PlanningAgentSession,
        openConversation: () -> AgentConversation,
    ): KronoResult<String> = try {
        val text = withTimeout(TIMEOUT_SECONDS.seconds) {
            loop.run(openConversation(), PlanningAgentPrompt.userMessage(request, session.now), session)
        }
        KronoResult.Success(text.trim().take(PlanningAgentPrompt.MAX_REPLY_LENGTH))
    } catch (timeout: TimeoutCancellationException) {
        Log.w(LOG_TAG, "El agente superó el tiempo límite de $TIMEOUT_SECONDS s", timeout)
        KronoResult.Error(AssistantError.Unavailable(timeout))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // Solo se registra la excepción: nunca el mensaje del usuario ni los títulos.
        Log.w(LOG_TAG, "Gemini no pudo atender la petición del agente", error)
        KronoResult.Error(AssistantError.Unavailable(error))
    }

    /** Parámetros del agente. */
    companion object {
        /**
         * Tiempo máximo de toda la petición. Son varias idas y vueltas con el modelo (cada una
         * de 5 a 12 s según las mediciones de F-12), así que es mayor que el de la explicación.
         * Con cinco tareas en un solo mensaje, 45 s no alcanzaban.
         */
        const val TIMEOUT_SECONDS = 90

        // Baja para que extraiga tareas con fidelidad al mensaje en vez de inventar.
        private const val TEMPERATURE = 0.2f

        // Cada ronda de herramientas paga el razonamiento del modelo; extraer tareas y llamar
        // herramientas no necesita más que el nivel bajo.
        private val THINKING_LEVEL = ThinkingLevel.LOW

        private const val LOG_TAG = "KronoAgent"
    }
}

/** [AgentConversation] sobre un chat de Firebase AI Logic. */
private class FirebaseAgentConversation(private val chat: Chat) : AgentConversation {

    override suspend fun send(message: String): AgentTurn = chat.sendMessage(message).toTurn()

    override suspend fun sendToolResults(results: List<ToolResult>): AgentTurn =
        chat.sendMessage(
            content(role = FUNCTION_RESPONSE_ROLE) {
                results.forEach { part(FunctionResponsePart(it.name, it.response)) }
            },
        ).toTurn()

    private fun GenerateContentResponse.toTurn(): AgentTurn =
        AgentTurn(text = text, toolCalls = functionCalls.map { ToolCall(it.name, it.args) })

    private companion object {
        // La guía de function calling de Firebase AI Logic envía las respuestas con rol "user".
        const val FUNCTION_RESPONSE_ROLE = "user"
    }
}
