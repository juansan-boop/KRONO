package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
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
    private var sessionCounter = 0
    private val finish = FinishTimerUseCase(repository, clock) { "sesion-${++sessionCounter}" }
    private val startTimer = StartTimerUseCase(repository, finish, clock)
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

        val session = (result as KronoResult.Success).data
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
}
