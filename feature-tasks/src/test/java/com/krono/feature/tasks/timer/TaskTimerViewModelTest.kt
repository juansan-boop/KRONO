package com.krono.feature.tasks.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FakeClock
import com.krono.core.domain.timer.FakeTimerRepository
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.ObserveActiveTimerUseCase
import com.krono.core.domain.timer.PauseTimerUseCase
import com.krono.core.domain.timer.ResumeTimerUseCase
import com.krono.core.domain.timer.StartTimerUseCase
import com.krono.feature.tasks.R
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de [TaskTimerViewModel] (F-08 CA-1 a CA-4), incluidos los toques repetidos mientras
 * una acción sigue en curso y los mensajes de error en español.
 *
 * El cronómetro refresca la pantalla en un bucle infinito, así que las pruebas avanzan el
 * tiempo con `runCurrent`/`advanceTimeBy` en lugar de `advanceUntilIdle`, y cada prueba
 * cancela al final los ViewModels creados para que `runTest` no se quede esperando ese bucle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskTimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = FakeClock(Instant.parse("2026-10-22T14:00:00Z"))

    private lateinit var tasks: FakeTaskRepository
    private lateinit var timers: FakeTimerRepository
    private lateinit var gated: GatedTimerRepository
    private lateinit var viewModel: TaskTimerViewModel
    private val createdViewModels = mutableListOf<TaskTimerViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tasks = FakeTaskRepository().apply { givenTasks(Task(id = "informe", title = "Informe", estimatedDuration = 60.minutes)) }
        timers = FakeTimerRepository()
        gated = GatedTimerRepository(timers)
        viewModel = createViewModel("informe")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrir_cargaLaTareaSinCronometro() = runTimerTest {
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("informe", state.task?.id)
        assertEquals(TimerStatus.IDLE, state.status)
    }

    @Test
    fun cuandoElListenerDeLaTareaFalla_muestraElErrorEnVezDeTareaNoEncontrada() = runTimerTest {
        tasks.observeFailure = TaskError.StorageError("Permiso denegado")
        val failing = createViewModel("informe")
        runCurrent()

        val state = failing.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isTaskNotFound)
        assertEquals(R.string.timer_error_load, state.errorMessageRes)
    }

    @Test
    fun alIniciar_corrigeElTiempoCadaSegundoDesdeElReloj() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()

        clock.advanceBy(90.seconds)
        tick()

        val state = viewModel.uiState.value
        assertEquals(TimerStatus.RUNNING, state.status)
        assertEquals(90.seconds, state.elapsed)
        assertEquals(0.025f, state.estimateProgress!!, 0.001f)
    }

    @Test
    fun alPausar_elTiempoSeDetiene() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        clock.advanceBy(10.minutes)
        viewModel.onPause()
        runCurrent()

        clock.advanceBy(5.minutes)
        tick()

        assertEquals(TimerStatus.PAUSED, viewModel.uiState.value.status)
        assertEquals(10.minutes, viewModel.uiState.value.elapsed)
    }

    @Test
    fun alFinalizar_guardaLaSesionYVuelveAReposo() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        clock.advanceBy(25.minutes)

        viewModel.onFinish()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(25.minutes, state.savedSession)
        assertFalse(state.isSavedSessionWithoutTask)
        assertEquals(25.minutes, timers.actualDurationByTask["informe"])
    }

    @Test
    fun alFinalizarUnaSesionDeMenosDeUnMinuto_noLaGuardaYAvisaQueEraMuyCorta() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        clock.advanceBy(13.seconds)

        viewModel.onFinish()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(TimerStatus.IDLE, state.status)
        assertTrue(state.isSessionTooShort)
        assertNull(state.savedSession)
        assertTrue(timers.sessions.isEmpty())
        assertNull(timers.actualDurationByTask["informe"])
    }

    @Test
    fun alFinalizarSiLaTareaYaNoExiste_avisaQueLaSesionNoSeSumo() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        clock.advanceBy(25.minutes)
        timers.deletedTasks += "informe"

        viewModel.onFinish()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(25.minutes, state.savedSession)
        assertTrue(state.isSavedSessionWithoutTask)
        assertEquals(1, timers.sessions.size)
    }

    @Test
    fun conCronometroHuerfanoDeUnaTareaBorrada_iniciaSinPedirConfirmacion() = runTimerTest {
        timers.givenActiveTimer(ActiveTimer("borrada", clock.now(), clock.now(), 0.minutes))
        runCurrent()

        viewModel.onStart()
        runCurrent()

        assertFalse(viewModel.uiState.value.isConfirmingSwitch)
        assertEquals(TimerStatus.RUNNING, viewModel.uiState.value.status)
        assertEquals("informe", timers.currentTimer?.taskId)
    }

    @Test
    fun alVolverALaPantalla_recuperaElCronometroEnCurso() = runTimerTest {
        timers.givenActiveTimer(ActiveTimer("informe", clock.now(), clock.now(), 0.minutes))
        clock.advanceBy(40.minutes)

        val reopened = createViewModel("informe")
        runCurrent()

        assertEquals(TimerStatus.RUNNING, reopened.uiState.value.status)
        assertEquals(40.minutes, reopened.uiState.value.elapsed)
    }

    @Test
    fun conCronometroEnOtraTarea_pideConfirmarYAlAceptarCambia() = runTimerTest {
        tasks.givenTasks(Task(id = "otra", title = "Otra"))
        timers.givenActiveTimer(ActiveTimer("otra", clock.now(), clock.now(), 0.minutes))
        runCurrent()
        clock.advanceBy(15.minutes)

        viewModel.onStart()
        runCurrent()
        assertTrue(viewModel.uiState.value.isConfirmingSwitch)
        assertEquals("otra", timers.currentTimer?.taskId)

        viewModel.onConfirmSwitch()
        runCurrent()

        assertFalse(viewModel.uiState.value.isConfirmingSwitch)
        assertEquals("informe", timers.currentTimer?.taskId)
        assertEquals(15.minutes, timers.actualDurationByTask["otra"])
    }

    @Test
    fun siFallaLaPersistencia_muestraUnMensajeGenericoSinDetallesTecnicos() = runTimerTest {
        runCurrent()
        timers.failure = TaskError.StorageError("Servicio de base de datos no disponible temporalmente.")

        viewModel.onStart()
        runCurrent()

        assertEquals(R.string.timer_error_generic, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isBusy)
        assertNull(timers.currentTimer)
    }

    @Test
    fun variosToquesEnFinalizar_finalizanUnaSolaVezSinErrorEspurio() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        clock.advanceBy(25.minutes)
        gated.gate = CompletableDeferred()

        repeat(REPEATED_TAPS) { viewModel.onFinish() }
        runCurrent()
        gated.gate?.complete(Unit)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, gated.finishCalls)
        assertEquals(25.minutes, timers.actualDurationByTask["informe"])
        assertEquals(25.minutes, state.savedSession)
        assertNull(state.errorMessageRes)
        assertFalse(state.isBusy)
    }

    @Test
    fun variosToquesEnIniciar_inicianUnaSolaVez() = runTimerTest {
        runCurrent()
        gated.gate = CompletableDeferred()

        repeat(REPEATED_TAPS) { viewModel.onStart() }
        runCurrent()
        gated.gate?.complete(Unit)
        runCurrent()

        assertEquals(1, gated.saveCalls)
        assertEquals(TimerStatus.RUNNING, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun variosToquesEnPausarYReanudar_soloSeAplicaElPrimero() = runTimerTest {
        runCurrent()
        viewModel.onStart()
        runCurrent()
        val savesAfterStart = gated.saveCalls
        gated.gate = CompletableDeferred()

        viewModel.onPause()
        viewModel.onResume()
        viewModel.onPause()
        runCurrent()
        gated.gate?.complete(Unit)
        runCurrent()

        assertEquals(savesAfterStart + 1, gated.saveCalls)
        assertEquals(TimerStatus.PAUSED, viewModel.uiState.value.status)
    }

    private fun runTimerTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            createdViewModels.forEach { it.viewModelScope.cancel() }
        }
    }

    private companion object {
        /** Toques casi simultáneos sobre un mismo botón, como los que reprodujo QA en el emulador. */
        const val REPEATED_TAPS = 6
    }

    private fun TestScope.tick() {
        advanceTimeBy(TaskTimerViewModel.TICK_INTERVAL.inWholeMilliseconds)
        runCurrent()
    }

    private fun createViewModel(taskId: String): TaskTimerViewModel {
        val finish = FinishTimerUseCase(gated, clock)
        return TaskTimerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TaskTimerViewModel.TASK_ID_NAV_ARG to taskId)),
            observeTaskById = ObserveTaskByIdUseCase(tasks),
            observeActiveTimer = ObserveActiveTimerUseCase(gated),
            startTimer = StartTimerUseCase(gated, tasks, finish, clock),
            pauseTimer = PauseTimerUseCase(gated, clock),
            resumeTimer = ResumeTimerUseCase(gated, clock),
            finishTimer = finish,
            clock = clock,
        ).also { createdViewModels += it }
    }
}
