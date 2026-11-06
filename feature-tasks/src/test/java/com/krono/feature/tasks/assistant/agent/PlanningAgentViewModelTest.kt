package com.krono.feature.tasks.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.agent.AskPlanningAgentUseCase
import com.krono.core.domain.assistant.agent.ConfirmAgentProposalUseCase
import com.krono.core.domain.assistant.agent.PlanningAgent
import com.krono.core.domain.assistant.agent.PlanningAgentSession
import com.krono.core.domain.assistant.agent.TaskDraftRequest
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTasksUseCase
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.UpsertTaskUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pruebas de [PlanningAgentViewModel] (F-28): proponer, confirmar y descartar. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanningAgentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedNow = LocalDateTime(2026, 11, 6, 14, 0)

    private lateinit var repository: FakeTaskRepository
    private var agentResult: (PlanningAgentSession) -> KronoResult<String> = { session ->
        session.createTask(
            TaskDraftRequest("Estudiar para el parcial", TaskCategory.ESTUDIO, TaskPriority.ALTA, LocalDateTime(2026, 11, 12, 7, 0), null),
        )
        KronoResult.Success("Empieza por estudiar.")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alEnviar_muestraLaPropuestaSinGuardarNada() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChange("tengo parcial el jueves")

        viewModel.onSend()
        advanceUntilIdle()

        val status = viewModel.uiState.value.status as AgentStatus.Proposed
        assertEquals("Estudiar para el parcial", status.proposal.drafts.single().request.title)
        assertEquals(0, repository.upsertCalls)
    }

    @Test
    fun sinMensaje_noSePuedeEnviar() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canSend)
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(AgentStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun alConfirmar_guardaLasTareasYLimpiaElMensaje() = runTest {
        val viewModel = proposedViewModel()

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(PlanningAgentUiState(status = AgentStatus.Saved(1)), viewModel.uiState.value)
        assertEquals(listOf("Estudiar para el parcial"), repository.storedTasks().map { it.title })
    }

    @Test
    fun siFallaElGuardado_conservaLaPropuestaConAviso() = runTest {
        val viewModel = proposedViewModel()
        repository.failure = TaskError.UnauthenticatedUser

        viewModel.onConfirm()
        advanceUntilIdle()

        val status = viewModel.uiState.value.status as AgentStatus.Proposed
        assertTrue(status.saveFailed)
        assertEquals("tengo parcial el jueves", viewModel.uiState.value.input)
    }

    @Test
    fun siElAgenteNoResponde_muestraNoDisponible() = runTest {
        agentResult = { KronoResult.Error(AssistantError.Unavailable()) }
        val viewModel = createViewModel()
        viewModel.onInputChange("tengo parcial el jueves")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(AgentStatus.Failed(AgentFailure.UNAVAILABLE), viewModel.uiState.value.status)
    }

    @Test
    fun alDescartar_vuelveAlEstadoInicialSinGuardar() = runTest {
        val viewModel = proposedViewModel()

        viewModel.onDiscard()

        assertEquals(AgentStatus.Idle, viewModel.uiState.value.status)
        assertEquals(0, repository.upsertCalls)
    }

    private fun TestScope.proposedViewModel(): PlanningAgentViewModel {
        val viewModel = createViewModel()
        viewModel.onInputChange("tengo parcial el jueves")
        viewModel.onSend()
        advanceUntilIdle()
        return viewModel
    }

    private fun createViewModel(): PlanningAgentViewModel {
        val agent = object : PlanningAgent {
            override suspend fun run(request: String, session: PlanningAgentSession) = agentResult(session)
        }
        var next = 0
        return PlanningAgentViewModel(
            observeTasksUseCase = ObserveTasksUseCase(repository),
            askPlanningAgent = AskPlanningAgentUseCase(
                agent = agent,
                buildPriorityPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase()),
                newTaskId = { "id-${++next}" },
            ),
            confirmAgentProposal = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository)),
        ).also { it.currentDateTimeProvider = { fixedNow } }
    }
}
