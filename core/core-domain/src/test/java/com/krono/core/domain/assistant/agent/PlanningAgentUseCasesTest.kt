package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.UpsertTaskUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [AskPlanningAgentUseCase] y [ConfirmAgentProposalUseCase] (F-28). */
class PlanningAgentUseCasesTest {

    private val now = LocalDateTime(2026, 11, 6, 14, 0)
    private val buildPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase())

    @Test
    fun unMensajeVacio_noLlamaAlAgente() = runTest {
        val agent = ScriptedAgent { KronoResult.Success("") }

        val result = ask(agent, "   ")

        assertTrue((result as KronoResult.Error).throwable is PlanningAgentError.InvalidRequest)
        assertEquals(0, agent.calls)
    }

    @Test
    fun cuandoElAgenteCreaTareas_devuelveLaPropuestaEstimadaYPlanificada() = runTest {
        val agent = ScriptedAgent { session ->
            session.createTask(draftRequest("Estudiar para el parcial"))
            KronoResult.Success("Te propongo estudiar primero.")
        }

        val proposal = (ask(agent, "tengo parcial el jueves") as KronoResult.Success).data

        assertEquals("Estudiar para el parcial", proposal.drafts.single().request.title)
        assertTrue(proposal.drafts.single().estimate != null)
        assertEquals(proposal.drafts.single().id, proposal.plan?.recommended?.task?.id)
        assertEquals("Te propongo estudiar primero.", proposal.message)
    }

    @Test
    fun cuandoElAgenteSoloPideUnDato_devuelveElMensajeSinTareas() = runTest {
        val agent = ScriptedAgent { KronoResult.Success("¿Qué día es tu parcial?") }

        val proposal = (ask(agent, "organízame la semana") as KronoResult.Success).data

        assertTrue(proposal.drafts.isEmpty())
        assertEquals("¿Qué día es tu parcial?", proposal.message)
    }

    @Test
    fun cuandoElAgenteNoProponeNada_devuelveNadaPropuesto() = runTest {
        val result = ask(ScriptedAgent { KronoResult.Success("  ") }, "hola")

        assertEquals(KronoResult.Error(PlanningAgentError.NothingProposed), result)
    }

    @Test
    fun cuandoElServicioFalla_propagaElError() = runTest {
        val failure = AssistantError.Unavailable()

        val result = ask(ScriptedAgent { KronoResult.Error(failure) }, "tengo parcial el jueves")

        assertEquals(KronoResult.Error(failure), result)
    }

    @Test
    fun confirmar_guardaCadaBorradorConSuIdYLaEstimacionDelUsuario() = runTest {
        val repository = FakeTaskRepository()
        val proposal = proposalWith(draftRequest("Informe", userMinutes = 90), draftRequest("Leer"))

        val result = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository))(proposal)

        assertEquals(KronoResult.Success(2), result)
        assertEquals(90.minutes, repository.storedTask("id-1")?.estimatedDuration)
        // La duración por defecto es un supuesto del plan, no una estimación: no se guarda.
        assertNull(repository.storedTask("id-2")?.estimatedDuration)
    }

    @Test
    fun confirmar_siFallaLaEscritura_devuelveElError() = runTest {
        val repository = FakeTaskRepository().apply { failure = TaskError.UnauthenticatedUser }
        val proposal = proposalWith(draftRequest("Informe"))

        val result = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository))(proposal)

        assertEquals(KronoResult.Error(TaskError.UnauthenticatedUser), result)
    }

    private suspend fun ask(agent: PlanningAgent, request: String): KronoResult<AgentProposal> {
        var next = 0
        return AskPlanningAgentUseCase(agent, buildPlan) { "id-${++next}" }(request, emptyList(), now)
    }

    private fun proposalWith(vararg requests: TaskDraftRequest): AgentProposal {
        var next = 0
        val session = PlanningAgentSession(emptyList(), now, buildPlan) { "id-${++next}" }
        requests.forEach { session.createTask(it) }
        return session.toProposal(null)
    }

    private fun draftRequest(title: String, userMinutes: Int? = null) = TaskDraftRequest(
        title = title,
        category = TaskCategory.ESTUDIO,
        priority = TaskPriority.ALTA,
        dueDate = LocalDateTime(2026, 11, 12, 7, 0),
        userEstimate = userMinutes?.minutes,
    )

    /** Agente que ejecuta [script] sobre la sesión en lugar de llamar a Gemini. */
    private class ScriptedAgent(
        private val script: (PlanningAgentSession) -> KronoResult<String>,
    ) : PlanningAgent {
        var calls = 0
            private set

        override suspend fun run(request: String, session: PlanningAgentSession): KronoResult<String> {
            calls++
            return script(session)
        }
    }
}
