package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
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
import kotlin.time.Duration.Companion.seconds

/** Pruebas de los casos de uso del cronómetro (F-08 CA-1 a CA-4). */
class TimerUseCasesTest {

    private val start = Instant.parse("2026-10-22T14:00:00Z")
    private val clock = FakeClock(start)
    private val repository = FakeTimerRepository()
    private val tasks = FakeTaskRepository().apply {
        givenTasks(Task(id = "tarea-1", title = "Informe"), Task(id = "tarea-2", title = "Parcial"))
    }
    private val finish = FinishTimerUseCase(repository, clock)
    private val startTimer = StartTimerUseCase(repository, tasks, finish, clock)
    private val pause = PauseTimerUseCase(repository, clock)
    private val resume = ResumeTimerUseCase(repository, clock)

    @Test
    fun alIniciar_quedaCorriendoDesdeAhora() = runTest {
        startTimer("tarea-1")

        val timer = repository.currentTimer!!
        assertEquals("tarea-1", timer.taskId)
        assertTrue(timer.isRunning)
        assertEquals(Duration.ZERO, timer.elapsedAt(start))
    }

    @Test
    fun elTiempoSeCalculaConMarcasDeTiempoNoConTicks() = runTest {
        startTimer("tarea-1")

        // CA-3: aunque nadie actualice el cronómetro, el tiempo sale de la marca de inicio.
        assertEquals(25.minutes, repository.currentTimer!!.elapsedAt(start + 25.minutes))
    }

    @Test
    fun laPausaNoCuentaTiempo() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(10.minutes)
        pause()
        clock.advanceBy(30.minutes)
        resume()
        clock.advanceBy(5.minutes)

        val timer = repository.currentTimer!!
        assertEquals(15.minutes, timer.elapsedAt(clock.now()))
    }

    @Test
    fun alFinalizar_registraLaSesionYSumaALaTarea() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(20.minutes)
        pause()
        clock.advanceBy(10.minutes)

        val result = finish()

        val finished = (result as KronoResult.Success).data
        val session = finished.session
        assertTrue(finished.addedToTask)
        assertEquals(1, repository.sessions.size)
        assertEquals(20.minutes, session.duration)
        assertEquals(start, session.startedAt)
        assertEquals(start + 30.minutes, session.endedAt)
        assertEquals(TimeSessionType.CRONOMETRO, session.type)
        assertEquals(20.minutes, repository.actualDurationByTask["tarea-1"])
        assertNull(repository.currentTimer)
    }

    @Test
    fun conOtroCronometroEnCurso_pideConfirmacion() = runTest {
        startTimer("tarea-1")

        val result = startTimer("tarea-2")

        assertEquals(KronoResult.Error(TimerError.AnotherTimerActive("tarea-1")), result)
        assertEquals("tarea-1", repository.currentTimer!!.taskId)
    }

    @Test
    fun alConfirmar_detieneYGuardaElAnteriorYIniciaElNuevo() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(12.minutes)

        startTimer("tarea-2", stopCurrent = true)

        assertEquals(12.minutes, repository.actualDurationByTask["tarea-1"])
        assertEquals("tarea-2", repository.currentTimer!!.taskId)
        assertEquals(clock.now(), repository.currentTimer!!.startedAt)
    }

    @Test
    fun iniciarLaMismaTareaEnPausa_laReanuda() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(5.minutes)
        pause()

        startTimer("tarea-1")

        val timer = repository.currentTimer!!
        assertTrue(timer.isRunning)
        assertEquals(5.minutes, timer.elapsedAt(clock.now()))
        assertTrue(repository.sessions.isEmpty())
    }

    @Test
    fun sinCronometro_pausarYFinalizarDevuelvenError() = runTest {
        assertEquals(KronoResult.Error(TimerError.NoActiveTimer), pause())
        assertEquals(KronoResult.Error(TimerError.NoActiveTimer), finish())
    }

    @Test
    fun pausarDosVeces_noCambiaElTiempo() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(30.seconds)
        pause()
        clock.advanceBy(1.minutes)
        pause()

        val timer = repository.currentTimer!!
        assertFalse(timer.isRunning)
        assertEquals(30.seconds, timer.elapsedAt(clock.now()))
    }

    @Test
    fun alReintentarTrasUnError_laSesionConservaElMismoIdYNoSeDuplica() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(15.minutes)
        repository.finishFailure = IllegalStateException("Permiso denegado")
        val failed = finish()
        repository.finishFailure = null
        clock.advanceBy(1.minutes)

        finish()

        assertTrue(failed is KronoResult.Error)
        assertEquals(2, repository.finishAttempts.size)
        assertEquals(repository.finishAttempts[0].id, repository.finishAttempts[1].id)
        assertEquals(1, repository.sessions.size)
    }

    @Test
    fun siLaTareaYaNoExiste_conservaLaSesionYAvisaQueNoSeSumo() = runTest {
        startTimer("tarea-1")
        clock.advanceBy(10.minutes)
        repository.deletedTasks += "tarea-1"

        val result = finish()

        val finished = (result as KronoResult.Success).data
        assertFalse(finished.addedToTask)
        assertEquals(10.minutes, finished.session.duration)
        assertEquals(listOf(finished.session), repository.sessions)
        assertNull(repository.actualDurationByTask["tarea-1"])
        assertNull(repository.currentTimer)
    }

    @Test
    fun conCronometroHuerfanoDeUnaTareaBorrada_iniciaSinPedirConfirmacionYLoDescarta() = runTest {
        repository.givenActiveTimer(ActiveTimer("borrada", start, start, Duration.ZERO))
        clock.advanceBy(20.minutes)

        val result = startTimer("tarea-1")

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals("tarea-1", repository.currentTimer!!.taskId)
        assertEquals(clock.now(), repository.currentTimer!!.startedAt)
        assertTrue(repository.sessions.isEmpty())
    }

    @Test
    fun siNoSePuedeComprobarLaTareaDelCronometroActivo_pideConfirmacionComoSiempre() = runTest {
        startTimer("tarea-1")
        tasks.failure = IllegalStateException("Sin conexión")

        val result = startTimer("tarea-2")

        assertEquals(KronoResult.Error(TimerError.AnotherTimerActive("tarea-1")), result)
    }
}
