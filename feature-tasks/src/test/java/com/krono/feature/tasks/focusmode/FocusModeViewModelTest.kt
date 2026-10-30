package com.krono.feature.tasks.focusmode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.FakeFocusTemplateRepository
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.GetActiveFocusTemplateUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FakeClock
import com.krono.core.domain.timer.FakeTimerRepository
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.FocusPhase
import com.krono.core.domain.timer.ObserveActiveTimerUseCase
import com.krono.core.domain.timer.PauseTimerUseCase
import com.krono.core.domain.timer.ResumeTimerUseCase
import com.krono.core.domain.timer.StartFocusBreakUseCase
import com.krono.core.domain.timer.StartFocusUseCase
import com.krono.core.domain.timer.StartTimerUseCase
import com.krono.core.domain.timer.TimeSessionType
import com.krono.feature.tasks.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas de [FocusModeViewModel] (F-09 CA-1 a CA-3) con un reloj falso: cuenta regresiva desde
 * marcas de tiempo, pausa, descansos, cero, finalización, tarea eliminada y otro cronómetro activo.
 *
 * El ViewModel refresca el tiempo en un bucle infinito, así que las pruebas avanzan el tiempo con
 * `runCurrent`/`advanceTimeBy` y cancelan los ViewModels al terminar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = FakeClock(Instant.parse("2026-10-30T09:00:00Z"))
    private val steps = listOf(
        Subtask("s1", "informe", "Reunir datos", isDone = true, position = 0),
        Subtask("s2", "informe", "Armar tablas", isDone = true, position = 1),
        Subtask("s3", "informe", "Redactar conclusiones", isDone = false, position = 2),
        Subtask("s4", "informe", "Revisar", isDone = false, position = 3),
    )

    private lateinit var tasks: FakeTaskRepository
    private lateinit var timers: FakeTimerRepository
    private lateinit var templates: FakeFocusTemplateRepository
    private val createdViewModels = mutableListOf<FocusModeViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tasks = FakeTaskRepository().apply {
            givenTasks(
                Task(id = "informe", title = "Informe", subtasks = steps),
                Task(id = "sin-pasos", title = "Sin pasos"),
                Task(id = "otra", title = "Otra"),
            )
        }
        timers = FakeTimerRepository()
        templates = FakeFocusTemplateRepository(DefaultFocusTemplates.all)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrir_iniciaElEnfoqueConLaPlantillaActivaYMuestraLaCuentaRegresiva() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Informe", state.task?.title)
        assertEquals(FocusPhase.WORKING, state.phase)
        assertEquals(45.minutes, state.remaining)
        assertEquals(45.minutes, state.blockDuration)
        assertEquals(45.minutes, timers.currentTimer?.focus?.blockDuration)
    }

    @Test
    fun usaLosMinutosDeLaPlantillaQueElUsuarioActivo() = focusTest {
        templates = FakeFocusTemplateRepository(
            listOf(DefaultFocusTemplates.deepStudy.copy(isActive = false), FocusTemplate("mia", "Mía", 30, 5, isActive = true)),
        )

        val viewModel = createViewModel("informe")
        runCurrent()

        assertEquals(30.minutes, viewModel.uiState.value.remaining)
        assertEquals(5.minutes, viewModel.uiState.value.templateBreak)
    }

    @Test
    fun sinPlantillasGuardadas_usaLaActivaSembrada() = focusTest {
        templates = FakeFocusTemplateRepository(emptyList())

        val viewModel = createViewModel("informe")
        runCurrent()

        assertEquals(45.minutes, viewModel.uiState.value.blockDuration)
    }

    @Test
    fun conLasSubtareasDeLaTarea_muestraElPasoActual() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(3, state.currentStep)
        assertEquals(4, state.totalSteps)
        assertEquals("Redactar conclusiones", state.currentStepTitle)
    }

    @Test
    fun unaTareaSinSubtareas_noMuestraPaso() = focusTest {
        val viewModel = createViewModel("sin-pasos")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(FocusPhase.WORKING, state.phase)
        assertEquals(0, state.totalSteps)
        assertNull(state.currentStep)
    }

    @Test
    fun conTodasLasSubtareasHechas_noHayPasoActual() = focusTest {
        tasks.givenTasks(Task(id = "hecha", title = "Hecha", subtasks = steps.map { it.copy(taskId = "hecha", isDone = true) }))
        val viewModel = createViewModel("hecha")
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.currentStep)
        assertTrue(state.areAllStepsDone)
    }

    @Test
    fun laCuentaRegresivaSeRecalculaDesdeElReloj() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()

        clock.advanceBy(10.minutes)
        tick()

        assertEquals(35.minutes, viewModel.uiState.value.remaining)
    }

    @Test
    fun alPausar_laCuentaSeDetieneYAlReanudarContinua() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(10.minutes)

        viewModel.onPause()
        runCurrent()
        clock.advanceBy(20.minutes)
        tick()

        assertEquals(FocusPhase.PAUSED, viewModel.uiState.value.phase)
        assertEquals(35.minutes, viewModel.uiState.value.remaining)

        viewModel.onResume()
        runCurrent()
        clock.advanceBy(5.minutes)
        tick()

        assertEquals(FocusPhase.WORKING, viewModel.uiState.value.phase)
        assertEquals(30.minutes, viewModel.uiState.value.remaining)
    }

    @Test
    fun elDescansoExtra_detieneElTrabajoYCuentaElDescanso() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(10.minutes)

        viewModel.onExtraBreak()
        runCurrent()
        clock.advanceBy(2.minutes)
        tick()

        val state = viewModel.uiState.value
        assertEquals(FocusPhase.ON_BREAK, state.phase)
        assertEquals(3.minutes, state.breakRemaining)
        assertEquals(35.minutes, state.remaining)
    }

    @Test
    fun alTerminarElDescanso_cambiaADescansoTerminado() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        viewModel.onExtraBreak()
        runCurrent()

        clock.advanceBy(6.minutes)
        tick()

        assertEquals(FocusPhase.BREAK_OVER, viewModel.uiState.value.phase)
    }

    @Test
    fun alLlegarACero_avisaYOfreceElDescansoDeLaPlantilla() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()

        clock.advanceBy(45.minutes)
        tick()

        val state = viewModel.uiState.value
        assertEquals(FocusPhase.BLOCK_COMPLETE, state.phase)
        assertEquals(Duration.ZERO, state.remaining)
        assertEquals(10.minutes, state.templateBreak)
    }

    @Test
    fun elDescansoDeLaPlantilla_empiezaConSuDuracion() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(46.minutes)
        tick()

        viewModel.onTakeTemplateBreak()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(FocusPhase.ON_BREAK, state.phase)
        assertEquals(10.minutes, state.breakRemaining)
    }

    @Test
    fun alFinalizar_guardaSesionEnfoqueConElTiempoTrabajadoSinPausasNiDescansos() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(20.minutes)
        viewModel.onExtraBreak()
        runCurrent()
        clock.advanceBy(30.minutes)

        viewModel.onFinish()
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.phase)
        assertEquals(20.minutes, state.savedSession)
        assertFalse(state.isSavedSessionWithoutTask)
        assertEquals(TimeSessionType.ENFOQUE, timers.sessions.single().type)
        assertEquals(20.minutes, timers.actualDurationByTask["informe"])
    }

    @Test
    fun despuesDeFinalizar_noReiniciaElEnfoqueSolo() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        viewModel.onFinish()
        runCurrent()

        clock.advanceBy(1.minutes)
        tick()

        assertNull(timers.currentTimer)
        assertEquals(1, timers.sessions.size)
    }

    @Test
    fun siLaTareaSeEliminaDuranteElEnfoque_laSesionSeGuardaIgualYSeAvisa() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(15.minutes)
        timers.deletedTasks += "informe"
        tasks.deleteTask("informe")
        runCurrent()

        assertTrue(viewModel.uiState.value.isTaskDeleted)
        assertEquals("Informe", viewModel.uiState.value.task?.title)

        viewModel.onFinish()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(15.minutes, state.savedSession)
        assertTrue(state.isSavedSessionWithoutTask)
        assertEquals(1, timers.sessions.size)
    }

    @Test
    fun unaTareaQueNoExiste_noIniciaNingunEnfoque() = focusTest {
        val viewModel = createViewModel("fantasma")
        runCurrent()

        assertTrue(viewModel.uiState.value.isTaskNotFound)
        assertNull(timers.currentTimer)
    }

    @Test
    fun conOtroCronometroActivo_pideConfirmarYNoLoDetiene() = focusTest {
        timers.givenActiveTimer(ActiveTimer("otra", clock.now(), clock.now(), Duration.ZERO))

        val viewModel = createViewModel("informe")
        runCurrent()

        assertTrue(viewModel.uiState.value.isConfirmingSwitch)
        assertEquals("otra", timers.currentTimer?.taskId)
    }

    @Test
    fun alConfirmarElCambio_guardaElAnteriorYEmpiezaElEnfoque() = focusTest {
        timers.givenActiveTimer(ActiveTimer("otra", clock.now(), clock.now(), Duration.ZERO))
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(7.minutes)

        viewModel.onConfirmSwitch()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isConfirmingSwitch)
        assertEquals(FocusPhase.WORKING, state.phase)
        assertEquals(7.minutes, timers.sessions.single().duration)
        assertEquals("informe", timers.currentTimer?.taskId)
    }

    @Test
    fun alVolverConElEnfoqueEnCurso_recuperaElTiempoDesdeElReloj() = focusTest {
        val first = createViewModel("informe")
        runCurrent()
        first.viewModelScope.cancel()
        clock.advanceBy(18.minutes)

        val reopened = createViewModel("informe")
        runCurrent()

        assertEquals(27.minutes, reopened.uiState.value.remaining)
        assertTrue(timers.sessions.isEmpty())
    }

    @Test
    fun alReabrirConElDescansoEnCurso_noLoCortaNiReinicia() = focusTest {
        val first = createViewModel("informe")
        runCurrent()
        first.onExtraBreak()
        runCurrent()
        first.viewModelScope.cancel()
        clock.advanceBy(1.minutes)

        val reopened = createViewModel("informe")
        runCurrent()

        assertEquals(FocusPhase.ON_BREAK, reopened.uiState.value.phase)
        assertEquals(4.minutes, reopened.uiState.value.breakRemaining)
    }

    @Test
    fun siLaEscrituraFalla_muestraUnErrorGenerico() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        timers.failure = IllegalStateException("sin permiso")

        viewModel.onPause()
        runCurrent()

        assertEquals(R.string.focus_mode_error_generic, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isBusy)

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun variosToquesEnFinalizar_soloGuardanUnaSesion() = focusTest {
        val viewModel = createViewModel("informe")
        runCurrent()
        clock.advanceBy(5.minutes)

        repeat(REPEATED_TAPS) { viewModel.onFinish() }
        runCurrent()

        assertEquals(1, timers.finishAttempts.size)
    }

    private fun focusTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            createdViewModels.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun TestScope.tick() {
        advanceTimeBy(FocusModeViewModel.TICK_INTERVAL.inWholeMilliseconds)
        runCurrent()
    }

    private fun createViewModel(taskId: String): FocusModeViewModel {
        val finish = FinishTimerUseCase(timers, clock)
        val startTimer = StartTimerUseCase(timers, tasks, finish, clock)
        return FocusModeViewModel(
            savedStateHandle = SavedStateHandle(mapOf(FocusModeViewModel.TASK_ID_NAV_ARG to taskId)),
            observeTaskById = ObserveTaskByIdUseCase(tasks),
            observeActiveTimer = ObserveActiveTimerUseCase(timers),
            getActiveFocusTemplate = GetActiveFocusTemplateUseCase(templates),
            startFocus = StartFocusUseCase(startTimer),
            startFocusBreak = StartFocusBreakUseCase(timers, clock),
            pauseTimer = PauseTimerUseCase(timers, clock),
            resumeTimer = ResumeTimerUseCase(timers, clock),
            finishTimer = finish,
            clock = clock,
        ).also { createdViewModels += it }
    }

    private companion object {
        /** Toques casi simultáneos sobre un mismo botón. */
        const val REPEATED_TAPS = 6
    }
}
