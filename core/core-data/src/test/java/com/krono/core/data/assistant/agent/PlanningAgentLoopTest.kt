package com.krono.core.data.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.agent.PlanningAgentSession
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del bucle de function calling y del manejo de fallos del agente (F-28). */
class PlanningAgentLoopTest {

    private val now = LocalDateTime(2026, 11, 6, 14, 0)
    private val session = PlanningAgentSession(
        currentTasks = emptyList(),
        now = now,
        buildPriorityPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase()),
        newTaskId = { "uuid-1" },
    )

    @Test
    fun atiendeLasHerramientasHastaQueElModeloRespondeConTexto() = runTest {
        val conversation = ScriptedConversation(
            AgentTurn(text = null, toolCalls = listOf(createTaskCall())),
            AgentTurn(text = null, toolCalls = listOf(ToolCall(PlanningToolDeclarations.BUILD_PLAN, emptyMap()))),
            AgentTurn(text = "Empieza por estudiar.", toolCalls = emptyList()),
        )

        val text = PlanningAgentLoop(PlanningToolDispatcher()).run(conversation, "hola", session)

        assertEquals("Empieza por estudiar.", text)
        assertEquals(1, session.drafts.size)
        assertEquals(listOf(listOf(PlanningToolDeclarations.CREATE_TASK), listOf(PlanningToolDeclarations.BUILD_PLAN)), conversation.sentResults)
    }

    @Test
    fun siElModeloNoDejaDePedirHerramientas_terminaSinTextoTrasElMaximoDeRondas() = runTest {
        val turns = List(PlanningAgentLoop.MAX_TOOL_ROUNDS + 1) { AgentTurn("parcial", listOf(createTaskCall())) }
        val conversation = ScriptedConversation(*turns.toTypedArray())

        val text = PlanningAgentLoop(PlanningToolDispatcher()).run(conversation, "hola", session)

        assertEquals("", text)
        assertEquals(PlanningAgentLoop.MAX_TOOL_ROUNDS, conversation.sentResults.size)
    }

    @Test
    fun unaPeticionMasLentaQueElLimite_devuelveNoDisponible() = runTest {
        val slow = object : AgentConversation {
            override suspend fun send(message: String): AgentTurn = awaitCancellation()
            override suspend fun sendToolResults(results: List<ToolResult>): AgentTurn = awaitCancellation()
        }

        val result = FirebaseAiPlanningAgent().runWith("hola", session) { slow }

        assertTrue((result as KronoResult.Error).throwable is AssistantError.Unavailable)
    }

    @Test
    fun unFalloDelServicio_devuelveNoDisponible() = runTest {
        val failing = object : AgentConversation {
            override suspend fun send(message: String): AgentTurn = throw IllegalStateException("cuota agotada")
            override suspend fun sendToolResults(results: List<ToolResult>): AgentTurn = error("no aplica")
        }

        val result = FirebaseAiPlanningAgent().runWith("hola", session) { failing }

        assertTrue((result as KronoResult.Error).throwable is AssistantError.Unavailable)
    }

    @Test
    fun elPrimerMensajeLlevaLaFechaActualParaResolverFechasRelativas() = runTest {
        val conversation = ScriptedConversation(AgentTurn("Listo.", emptyList()))

        FirebaseAiPlanningAgent().runWith("tengo parcial el jueves", session) { conversation }

        val message = conversation.firstMessage
        assertTrue(message.contains("viernes 2026-11-06T14:00"))
        assertTrue(message.contains("tengo parcial el jueves"))
    }

    private fun createTaskCall() = ToolCall(
        PlanningToolDeclarations.CREATE_TASK,
        mapOf("title" to JsonPrimitive("Estudiar para el parcial"), "category" to JsonPrimitive("ESTUDIO")),
    )

    /** Conversación que devuelve los turnos en orden y registra lo que recibe. */
    private class ScriptedConversation(vararg turns: AgentTurn) : AgentConversation {
        private val pending = ArrayDeque(turns.toList())
        var firstMessage: String = ""
            private set
        val sentResults = mutableListOf<List<String>>()

        override suspend fun send(message: String): AgentTurn {
            firstMessage = message
            return pending.removeFirst()
        }

        override suspend fun sendToolResults(results: List<ToolResult>): AgentTurn {
            sentResults += results.map { it.name }
            return pending.removeFirst()
        }
    }
}
