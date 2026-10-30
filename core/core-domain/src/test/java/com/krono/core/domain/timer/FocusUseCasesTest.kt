package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.FakeFocusTemplateRepository
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.GetActiveFocusTemplateUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Pruebas del modo enfoque en el dominio (F-09 CA-1 a CA-3): fases, descanso y sesión `ENFOQUE`. */
class FocusUseCasesTest {

    private val start = Instant.parse("2026-10-30T09:00:00Z")
    private val clock = FakeClock(start)
    private val repository = FakeTimerRepository()
    private val tasks = FakeTaskRepository().apply {
        givenTasks(Task(id = "tarea-1", title = "Informe"), Task(id = "tarea-2", title = "Parcial"))
    }
    private val finish = FinishTimerUseCase(repository, clock)
    private val startTimer = StartTimerUseCase(repository, tasks, finish, clock)
    private val startFocus = StartFocusUseCase(startTimer)
    private val startBreak = StartFocusBreakUseCase(repository, clock)
    private val pause = PauseTimerUseCase(repository, clock)
    private val resume = ResumeTimerUseCase(repository, clock)
    private val template = FocusTemplate("t", "Prueba", focusMinutes = 45, breakMinutes = 10, isActive = true)
    private val extraBreak = FocusDefaults.extraBreak

    private fun timer() = repository.currentTimer!!

    @Test
    fun alIniciarEnfoque_guardaElBloqueDeLaPlantillaYCorre() = runTest {
        startFocus("tarea-1", template)

        val focus = timer().focus!!
        assertEquals(45.minutes, focus.blockDuration)
        assertEquals(10.minutes, focus.breakDuration)
        assertEquals(FocusPhase.WORKING, timer().focusPhaseAt(start))
        assertEquals(45.minutes, timer().focusRemainingAt(start))
    }

    @Test
    fun laCuentaRegresivaSaleDeMarcasDeTiempo() = runTest {
        startFocus("tarea-1", template)

        assertEquals(20.minutes, timer().focusRemainingAt(start + 25.minutes))
    }

    @Test
    fun alLlegarACero_laFaseEsBloqueCompletoYElTiempoNoSuperaElBloque() = runTest {
        startFocus("tarea-1", template)
        val later = start + 120.minutes

        assertEquals(FocusPhase.BLOCK_COMPLETE, timer().focusPhaseAt(later))
        assertEquals(Duration.ZERO, timer().focusRemainingAt(later))
        assertEquals(45.minutes, timer().elapsedAt(later))
    }

    @Test
    fun laPausaNoGastaLaCuentaRegresiva() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(10.minutes)
        pause()
        clock.advanceBy(30.minutes)

        assertEquals(FocusPhase.PAUSED, timer().focusPhaseAt(clock.now()))
        assertEquals(35.minutes, timer().focusRemainingAt(clock.now()))
    }

    @Test
    fun elDescansoExtraPausaElTrabajoYNoCuenta() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(10.minutes)

        startBreak(extraBreak)

        assertEquals(FocusPhase.ON_BREAK, timer().focusPhaseAt(clock.now()))
        assertEquals(5.minutes, timer().breakRemainingAt(clock.now()))
        assertEquals(2.minutes, timer().breakRemainingAt(clock.now() + 3.minutes))
        assertEquals(10.minutes, timer().elapsedAt(clock.now() + 3.minutes))
        assertEquals(35.minutes, timer().focusRemainingAt(clock.now() + 3.minutes))
    }

    @Test
    fun alTerminarElDescanso_laFaseEsDescansoTerminadoYElTrabajoSigueDetenido() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(10.minutes)
        startBreak(extraBreak)

        val later = clock.now() + 6.minutes

        assertEquals(FocusPhase.BREAK_OVER, timer().focusPhaseAt(later))
        assertEquals(Duration.ZERO, timer().breakRemainingAt(later))
        assertEquals(10.minutes, timer().elapsedAt(later))
    }

    @Test
    fun reanudarDuranteElDescanso_loTerminaYVuelveAlTrabajo() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(10.minutes)
        startBreak(extraBreak)
        clock.advanceBy(2.minutes)

        resume()

        assertNull(timer().focus!!.breakInProgress)
        assertEquals(FocusPhase.WORKING, timer().focusPhaseAt(clock.now()))
        assertEquals(15.minutes, timer().focusRemainingAt(clock.now() + 20.minutes))
    }

    @Test
    fun descansarAlCompletarElBloque_preparaOtroBloque() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(50.minutes)

        startBreak(template.breakMinutes.minutes)
        clock.advanceBy(10.minutes)
        resume()

        assertEquals(45.minutes, timer().elapsedAt(clock.now()))
        assertEquals(45.minutes, timer().focusRemainingAt(clock.now()))
        assertEquals(30.minutes, timer().focusRemainingAt(clock.now() + 15.minutes))
    }

    @Test
    fun descansarSinEnfoqueActivoDevuelveError() = runTest {
        assertEquals(TimerError.NoActiveTimer, (startBreak(extraBreak) as KronoResult.Error).throwable)

        startTimer("tarea-1")

        assertEquals(TimerError.NotFocusSession, (startBreak(extraBreak) as KronoResult.Error).throwable)
    }

    @Test
    fun alFinalizar_registraSesionEnfoqueSoloConElTiempoTrabajado() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(20.minutes)
        startBreak(extraBreak)
        clock.advanceBy(30.minutes)

        val finished = (finish() as KronoResult.Success).data

        assertEquals(TimeSessionType.ENFOQUE, finished.session.type)
        assertEquals(20.minutes, finished.session.duration)
        assertEquals(20.minutes, repository.actualDurationByTask["tarea-1"])
        assertNull(repository.currentTimer)
    }

    @Test
    fun alFinalizarConElBloqueCompleto_noSumaElTiempoPasadoDeCero() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(120.minutes)

        val finished = (finish() as KronoResult.Success).data

        assertEquals(45.minutes, finished.session.duration)
    }

    @Test
    fun alFinalizarUnCronometro_laSesionSigueSiendoCronometro() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(5.minutes)

        val finished = (finish() as KronoResult.Success).data

        assertEquals(TimeSessionType.CRONOMETRO, finished.session.type)
    }

    @Test
    fun siLaTareaSeBorraDuranteElEnfoque_laSesionSeGuardaSinSumarse() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(15.minutes)
        repository.deletedTasks += "tarea-1"

        val finished = (finish() as KronoResult.Success).data

        assertEquals(15.minutes, finished.session.duration)
        assertFalse(finished.addedToTask)
        assertEquals(1, repository.sessions.size)
    }

    @Test
    fun iniciarEnfoqueConOtroCronometroActivo_pideConfirmacion() = runTest {
        startTimer("tarea-2")

        val result = startFocus("tarea-1", template)

        assertEquals(TimerError.AnotherTimerActive("tarea-2"), (result as KronoResult.Error).throwable)
    }

    @Test
    fun iniciarEnfoqueConConfirmacion_guardaElAnteriorYEmpiezaElEnfoque() = runTest {
        startTimer("tarea-2")
        clock.advanceBy(5.minutes)

        startFocus("tarea-1", template, stopCurrent = true)

        assertEquals(5.minutes, repository.sessions.single().duration)
        assertEquals("tarea-1", timer().taskId)
        assertTrue(timer().focus != null)
    }

    @Test
    fun iniciarEnfoqueDeLaMismaTareaYaEnCurso_noLoReinicia() = runTest {
        startFocus("tarea-1", template)
        clock.advanceBy(10.minutes)
        startBreak(extraBreak)
        val before = timer()

        val result = startFocus("tarea-1", template)

        assertTrue(result is KronoResult.Success)
        assertEquals(before, timer())
    }

    @Test
    fun iniciarEnfoqueSobreElCronometroDeLaMismaTarea_pideConfirmacion() = runTest {
        startTimer("tarea-1")

        val result = startFocus("tarea-1", template)

        assertEquals(TimerError.AnotherTimerActive("tarea-1"), (result as KronoResult.Error).throwable)
    }

    @Test
    fun laPlantillaActivaEsLaQueElUsuarioElige() = runTest {
        val custom = FocusTemplate("c", "Mía", 30, 5, isActive = true)
        val templates = FakeFocusTemplateRepository(listOf(DefaultFocusTemplates.deepStudy.copy(isActive = false), custom))

        assertEquals(custom, GetActiveFocusTemplateUseCase(templates)())
    }

    @Test
    fun sinPlantillasOConError_usaLaActivaSembrada() = runTest {
        val empty = FakeFocusTemplateRepository()
        val failing = FakeFocusTemplateRepository(listOf(DefaultFocusTemplates.quickSprint)).apply {
            failure = IllegalStateException("sin permiso")
        }

        assertEquals(DefaultFocusTemplates.deepStudy, GetActiveFocusTemplateUseCase(empty)())
        assertEquals(DefaultFocusTemplates.deepStudy, GetActiveFocusTemplateUseCase(failing)())
    }

    @Test
    fun sinNingunaMarcadaActiva_usaLaPrimera() = runTest {
        val noneActive = FakeFocusTemplateRepository(listOf(DefaultFocusTemplates.quickSprint.copy(isActive = false)))

        assertEquals("default-quick-sprint", GetActiveFocusTemplateUseCase(noneActive)().id)
    }
}
