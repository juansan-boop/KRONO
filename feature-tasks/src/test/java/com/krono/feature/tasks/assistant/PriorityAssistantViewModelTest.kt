package com.krono.feature.tasks.assistant

import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.ExplainPriorityUseCase
import com.krono.core.domain.assistant.FakePriorityExplanationGenerator
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTasksUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskPriority
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pruebas de [PriorityAssistantViewModel] (F-12 CA-1, D-5 opción híbrida). */
@OptIn(ExperimentalCoroutinesApi::class)
class PriorityAssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedNow = LocalDateTime(2026, 10, 22, 14, 0)

    private lateinit var repository: FakeTaskRepository
    private lateinit var generator: FakePriorityExplanationGenerator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
        generator = FakePriorityExplanationGenerator(explanation = "Empieza por el informe.")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sinTareasPendientes_muestraEstadoVacioYNoLlamaALaIa() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertTrue(generator.requests.isEmpty())
    }

    @Test
    fun conTareas_recomiendaLaMasUrgenteYMuestraLaExplicacionDeLaIa() = runTest {
        repository.givenTasks(
            task("informe", dueHour = 18, priority = TaskPriority.ALTA),
            task("lectura", dueHour = 23, priority = TaskPriority.BAJA, dueDay = 25),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("informe", state.plan?.recommended?.task?.id)
        assertEquals(listOf("lectura"), state.plan?.sequence?.map { it.task.id })
        assertEquals(ExplanationState.Generated("Empieza por el informe."), state.explanation)
    }

    @Test
    fun cuandoElListenerFalla_muestraErrorYAlRefrescarReabreLaObservacion() = runTest {
        repository.givenTasks(task("informe", dueHour = 18))
        repository.observeFailure = TaskError.StorageError("Permiso denegado")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasError)

        repository.observeFailure = null
        viewModel.onRefresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasError)
        assertEquals("informe", viewModel.uiState.value.plan?.recommended?.task?.id)
        assertEquals(2, repository.observeTasksSubscriptions)
    }

    @Test
    fun siLaIaFalla_usaLaExplicacionDePlantilla() = runTest {
        generator.failure = AssistantError.Unavailable()
        repository.givenTasks(task("informe", dueHour = 18))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("informe", viewModel.uiState.value.plan?.recommended?.task?.id)
        assertEquals(ExplanationState.Fallback, viewModel.uiState.value.explanation)
    }

    @Test
    fun mientrasLaIaTarda_muestraLaRecomendacionYLaSecuenciaYLuegoReemplazaElMotivo() = runTest {
        val gate = CompletableDeferred<Unit>()
        generator.gate = gate
        repository.givenTasks(
            task("informe", dueHour = 18, priority = TaskPriority.ALTA),
            task("lectura", dueHour = 23, priority = TaskPriority.BAJA, dueDay = 25),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        val waiting = viewModel.uiState.value
        assertFalse(waiting.isLoading)
        assertEquals("informe", waiting.plan?.recommended?.task?.id)
        assertEquals(listOf("lectura"), waiting.plan?.sequence?.map { it.task.id })
        assertEquals(ExplanationState.Loading, waiting.explanation)

        gate.complete(Unit)
        advanceUntilIdle()

        val answered = viewModel.uiState.value
        assertEquals(waiting.plan, answered.plan)
        assertEquals(ExplanationState.Generated("Empieza por el informe."), answered.explanation)
    }

    @Test
    fun siLaIaFallaTrasLaEspera_conservaElPlanYPasaALaPlantilla() = runTest {
        val gate = CompletableDeferred<Unit>()
        generator.gate = gate
        generator.failure = AssistantError.Unavailable()
        repository.givenTasks(task("informe", dueHour = 18))
        val viewModel = createViewModel()
        advanceUntilIdle()
        val waiting = viewModel.uiState.value.plan

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(waiting, viewModel.uiState.value.plan)
        assertEquals(ExplanationState.Fallback, viewModel.uiState.value.explanation)
    }

    @Test
    fun siLaRecomendacionNoCambia_noVuelveAPedirExplicacion() = runTest {
        repository.givenTasks(task("informe", dueHour = 18, priority = TaskPriority.ALTA))
        createViewModel()
        advanceUntilIdle()

        repository.givenTasks(task("otra", dueHour = 22, priority = TaskPriority.BAJA, dueDay = 28))
        advanceUntilIdle()

        assertEquals(1, generator.requests.size)
    }

    @Test
    fun siCambiaLaRecomendacion_pideUnaExplicacionNueva() = runTest {
        repository.givenTasks(task("lectura", dueHour = 23, dueDay = 25))
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.givenTasks(task("urgente", dueHour = 16, priority = TaskPriority.ALTA))
        advanceUntilIdle()

        assertEquals("urgente", viewModel.uiState.value.plan?.recommended?.task?.id)
        assertEquals(2, generator.requests.size)
    }

    @Test
    fun alCompletarLaUltimaTarea_quedaVacio() = runTest {
        repository.givenTasks(task("informe", dueHour = 18))
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.givenTasks(task("informe", dueHour = 18).copy(isCompleted = true))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.plan)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun alRecalcular_pideLaExplicacionDeNuevo() = runTest {
        repository.givenTasks(task("informe", dueHour = 18))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRefresh()
        advanceUntilIdle()

        assertEquals(2, generator.requests.size)
    }

    private fun createViewModel() = PriorityAssistantViewModel(
        observeTasksUseCase = ObserveTasksUseCase(repository),
        buildPriorityPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase()),
        explainPriority = ExplainPriorityUseCase(generator),
    ).apply { currentDateTimeProvider = { fixedNow } }

    private fun task(
        id: String,
        dueHour: Int,
        priority: TaskPriority = TaskPriority.MEDIA,
        dueDay: Int = 22,
    ) = Task(
        id = id,
        title = "Tarea $id",
        priority = priority,
        dueDate = LocalDateTime(2026, 10, dueDay, dueHour, 0),
    )
}
