package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.sync.SyncStatus
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

        assertTrue(result is KronoResult.Success)
        val batch = (result as KronoResult.Success).data
        assertEquals(2, batch.successfulCount)
        assertEquals(2, batch.confirmedTasks.size)
        assertTrue(batch.isAllSuccessful)
        assertEquals(90.minutes, repository.storedTask("id-1")?.estimatedDuration)
        // La duración por defecto es un supuesto del plan, no una estimación: no se guarda.
        assertNull(repository.storedTask("id-2")?.estimatedDuration)
    }

    @Test
    fun confirmar_siFallaLaEscritura_registraTareasFallidasEnBatchResult() = runTest {
        val repository = FakeTaskRepository().apply { failure = TaskError.UnauthenticatedUser }
        val proposal = proposalWith(draftRequest("Informe"))

        val result = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository))(proposal)

        assertTrue(result is KronoResult.Success)
        val batch = (result as KronoResult.Success).data
        assertEquals(1, batch.failedTasks.size)
        assertTrue(batch.isAllFailed)
        assertEquals("Informe", batch.failedTasks.first().task.title)
    }

    @Test
    fun guardadoSecuencial_conMezclaDeConfirmedPendingSyncYFailed_reportaExactoEnBatchSaveResult() = runTest {
        val repository = FakeTaskRepository()
        val proposal = proposalWith(
            draftRequest("Tarea 1"),
            draftRequest("Tarea 2"),
            draftRequest("Tarea 3"),
        )
        // Tarea 1: confirmada
        repository.taskSyncStatusMap["id-1"] = SyncStatus.CONFIRMED
        // Tarea 2: pendiente de sincronización
        repository.taskSyncStatusMap["id-2"] = SyncStatus.PENDING_SYNC
        // Tarea 3: fallo de red
        repository.taskFailureMap["id-3"] = TaskError.StorageError("Fallo de conexión remoto")

        val result = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository))(proposal)

        assertTrue(result is KronoResult.Success)
        val batch = (result as KronoResult.Success).data
        assertEquals(3, batch.totalCount)
        assertEquals(2, batch.successfulCount)
        assertTrue(batch.isPartial)

        assertEquals(1, batch.confirmedTasks.size)
        assertEquals("id-1", batch.confirmedTasks[0].id)
        assertEquals(SyncStatus.CONFIRMED, batch.confirmedTasks[0].syncStatus)

        assertEquals(1, batch.pendingSyncTasks.size)
        assertEquals("id-2", batch.pendingSyncTasks[0].id)
        assertEquals(SyncStatus.PENDING_SYNC, batch.pendingSyncTasks[0].syncStatus)

        assertEquals(1, batch.failedTasks.size)
        assertEquals("id-3", batch.failedTasks[0].task.id)
        assertTrue(batch.failedTasks[0].reason.contains("Fallo de conexión"))
    }

    @Test
    fun retryTasks_conservaIdsOriginales_yGuardaSoloLasTareasFallidas() = runTest {
        val repository = FakeTaskRepository()
        val useCase = ConfirmAgentProposalUseCase(UpsertTaskUseCase(repository))

        val proposal = proposalWith(
            draftRequest("Tarea A"),
            draftRequest("Tarea B"),
        )
        repository.taskFailureMap["id-2"] = TaskError.StorageError("Error temporal")

        val initialResult = useCase(proposal)
        val initialBatch = (initialResult as KronoResult.Success).data
        assertEquals(1, initialBatch.confirmedTasks.size)
        assertEquals(1, initialBatch.failedTasks.size)

        // Limpiamos el error para el reintento
        repository.taskFailureMap.remove("id-2")
        val failedToRetry = initialBatch.failedTasks.map { it.task }

        val retryResult = useCase.retryTasks(failedToRetry)
        assertTrue(retryResult is KronoResult.Success)
        val retryBatch = (retryResult as KronoResult.Success).data

        assertEquals(1, retryBatch.confirmedTasks.size)
        assertEquals("id-2", retryBatch.confirmedTasks[0].id)
        assertTrue(retryBatch.failedTasks.isEmpty())
        assertTrue(retryBatch.isAllSuccessful)
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
