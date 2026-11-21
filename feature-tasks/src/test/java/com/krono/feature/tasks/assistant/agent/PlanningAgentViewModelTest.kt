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
import com.krono.core.domain.sync.SyncStatus
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

        val status = viewModel.uiState.value.status as AgentStatus.Saved
        assertEquals(1, status.count)
        assertEquals(listOf("Estudiar para el parcial"), repository.storedTasks().map { it.title })
    }

    @Test
    fun guardadoSecuencial_conMezclaDeConfirmedPendingSyncYFailed_reportaEnUiState() = runTest {
        agentResult = { session ->
            session.createTask(TaskDraftRequest("Tarea 1", TaskCategory.ESTUDIO, TaskPriority.ALTA, LocalDateTime(2026, 11, 12, 7, 0), null))
            session.createTask(TaskDraftRequest("Tarea 2", TaskCategory.ESTUDIO, TaskPriority.MEDIA, LocalDateTime(2026, 11, 13, 7, 0), null))
            session.createTask(TaskDraftRequest("Tarea 3", TaskCategory.ESTUDIO, TaskPriority.BAJA, LocalDateTime(2026, 11, 14, 7, 0), null))
            KronoResult.Success("Propuesta múltiple.")
        }
        val viewModel = proposedViewModel()

        repository.taskSyncStatusMap["id-1"] = SyncStatus.CONFIRMED
        repository.taskSyncStatusMap["id-2"] = SyncStatus.PENDING_SYNC
        repository.taskFailureMap["id-3"] = TaskError.StorageError("Error de prueba")

        viewModel.onConfirm()
        advanceUntilIdle()

        val status = viewModel.uiState.value.status as AgentStatus.Saved
        assertEquals(1, status.result.confirmedTasks.size)
        assertEquals("id-1", status.result.confirmedTasks[0].id)
        assertEquals(1, status.result.pendingSyncTasks.size)
        assertEquals("id-2", status.result.pendingSyncTasks[0].id)
        assertEquals(1, status.result.failedTasks.size)
        assertEquals("id-3", status.result.failedTasks[0].task.id)
    }

    @Test
    fun retryFailedTasks_conservaIdentificadoresYReintentaSoloFallidas() = runTest {
        agentResult = { session ->
            session.createTask(TaskDraftRequest("Tarea Éxito", TaskCategory.ESTUDIO, TaskPriority.ALTA, LocalDateTime(2026, 11, 12, 7, 0), null))
            session.createTask(TaskDraftRequest("Tarea Fallo", TaskCategory.ESTUDIO, TaskPriority.ALTA, LocalDateTime(2026, 11, 13, 7, 0), null))
            KronoResult.Success("Propuesta con fallo.")
        }
        val viewModel = proposedViewModel()

        repository.taskSyncStatusMap["id-1"] = SyncStatus.CONFIRMED
        repository.taskFailureMap["id-2"] = TaskError.StorageError("Error transitorio")

        viewModel.onConfirm()
        advanceUntilIdle()

        val statusInitial = viewModel.uiState.value.status as AgentStatus.Saved
        assertEquals(1, statusInitial.result.confirmedTasks.size)
        assertEquals(1, statusInitial.result.failedTasks.size)

        // Ahora resolvemos el fallo y ejecutamos retryFailedTasks()
        repository.taskFailureMap.remove("id-2")
        repository.taskSyncStatusMap["id-2"] = SyncStatus.CONFIRMED

        viewModel.retryFailedTasks()
        advanceUntilIdle()

        val statusRetried = viewModel.uiState.value.status as AgentStatus.Saved
        assertEquals(2, statusRetried.result.confirmedTasks.size)
        assertTrue(statusRetried.result.failedTasks.isEmpty())
        val storedIds = repository.storedTasks().map { it.id }.toSet()
        assertTrue(storedIds.contains("id-1"))
        assertTrue(storedIds.contains("id-2"))
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
