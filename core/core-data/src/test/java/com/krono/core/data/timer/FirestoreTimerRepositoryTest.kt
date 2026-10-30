package com.krono.core.data.timer

import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.data.task.FakeTaskRemoteDataSource
import com.krono.core.data.task.FirestoreTaskRepository
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.FocusBlock
import com.krono.core.domain.timer.FocusBreak
import com.krono.core.domain.timer.FinishedSession
import com.krono.core.domain.timer.StartTimerUseCase
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import com.krono.core.domain.timer.TimerError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de [FirestoreTimerRepository] y de la serialización del cronómetro (F-08): errores
 * locales, escrituras sin confirmación del servidor, sesiones de tareas borradas, finalizaciones
 * repetidas o simultáneas y lecturas sin red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTimerRepositoryTest {

    private val user = User(id = "user-123", email = "estudiante@krono.app")
    private val start = Instant.parse("2026-10-22T14:00:00Z")

    private lateinit var auth: FakeAuthRemoteDataSource
    private lateinit var remote: FakeTimerRemoteDataSource
    private lateinit var repository: FirestoreTimerRepository

    @Before
    fun setUp() {
        auth = FakeAuthRemoteDataSource()
        remote = FakeTimerRemoteDataSource()
        repository = FirestoreTimerRepository(remote, auth)
    }

    @Test
    fun guardarYLeer_conservaTodosLosCampos() = runTest {
        auth.user.value = user
        val timer = ActiveTimer("tarea-1", start, runningSince = null, accumulated = 12.minutes)

        repository.saveActiveTimer(timer)

        assertEquals(KronoResult.Success(timer), repository.getActiveTimer())
        assertEquals(timer, repository.observeActiveTimer().first())
    }

    @Test
    fun guardarYLeerUnEnfoque_conservaElBloqueYElDescanso() = runTest {
        auth.user.value = user
        val focus = FocusBlock(45.minutes, 10.minutes, targetElapsed = 90.minutes, breakInProgress = FocusBreak(start, 5.minutes))
        val timer = ActiveTimer("tarea-1", start, runningSince = null, accumulated = 12.minutes, focus = focus)

        repository.saveActiveTimer(timer)

        assertEquals(KronoResult.Success(timer), repository.getActiveTimer())
        assertEquals(timer, repository.observeActiveTimer().first())
    }

    @Test
    fun sinSesion_noGuardaYDevuelveError() = runTest {
        val result = repository.saveActiveTimer(ActiveTimer("tarea-1", start, start, 0.minutes))

        assertEquals(KronoResult.Error(TaskError.UnauthenticatedUser), result)
        assertNull(remote.timers[user.id])
    }

    @Test
    fun guardarConErrorLocal_devuelveError() = runTest {
        auth.user.value = user
        remote.writeFailure = firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val result = repository.saveActiveTimer(ActiveTimer("tarea-1", start, start, 0.minutes))

        assertTrue(result is KronoResult.Error)
    }

    @Test
    fun guardarSinConfirmacionDelServidor_devuelveSuccessSinBloquear() = runTest {
        auth.user.value = user
        remote.serverConfirmation = CompletableDeferred()
        val timer = ActiveTimer("tarea-1", start, start, 0.minutes)

        val result = backgroundScope.async { repository.saveActiveTimer(timer) }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
        assertEquals(KronoResult.Success(timer), repository.getActiveTimer())
    }

    @Test
    fun alFinalizar_sumaALaTareaSiTodaviaExiste() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        val finished = remote.finished.single()
        assertEquals(25.minutes.inWholeMilliseconds, finished.durationMillis)
        assertEquals("CRONOMETRO", finished.session[TimeSessionDto.FIELD_TYPE])
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
        assertEquals(KronoResult.Success(FinishedSession(session(taskId = "tarea-1"), addedToTask = true)), result)
    }

    @Test
    fun siLaTareaYaNoExiste_elIncrementoFallaPeroLaSesionSeConservaYElCronometroSeBorra() = runTest {
        auth.user.value = user
        givenRunningTimer("borrada")

        val result = repository.finishActiveTimer(session(taskId = "borrada"))

        assertEquals(KronoResult.Success(FinishedSession(session(taskId = "borrada"), addedToTask = false)), result)
        assertEquals(listOf(session(taskId = "borrada").id), remote.sessionsById.keys.toList())
        assertTrue(remote.incrementsByTask.isEmpty())
        assertNull(remote.timers[user.id])
    }

    @Test
    fun siElLoteFallaLocalmente_devuelveErrorYConservaElCronometro() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        remote.writeFailure = firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertTrue(result is KronoResult.Error)
        assertTrue(remote.sessionsById.isEmpty())
        assertNotNull(remote.timers[user.id])
    }

    @Test
    fun alFinalizarSinConfirmacionDelServidor_devuelveSuccessSinBloquear() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        remote.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async { repository.finishActiveTimer(session(taskId = "tarea-1")) }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertTrue(result.await() is KronoResult.Success)
        assertEquals(listOf(session(taskId = "tarea-1").id), remote.sessionsById.keys.toList())
    }

    @Test
    fun alReintentarTrasUnError_noDuplicaLaSesion() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        remote.writeFailure = firestoreError(FirebaseFirestoreException.Code.INVALID_ARGUMENT)
        val failed = repository.finishActiveTimer(session(taskId = "tarea-1"))
        remote.writeFailure = null

        repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertTrue(failed is KronoResult.Error)
        assertEquals(listOf(session(taskId = "tarea-1").id), remote.sessionsById.keys.toList())
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
    }

    @Test
    fun variasFinalizacionesALaVez_registranUnaSesionYUnSoloIncremento() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        remote.serverConfirmation = CompletableDeferred()

        val results = List(CONCURRENT_FINISHES) {
            backgroundScope.async { repository.finishActiveTimer(session(taskId = "tarea-1")) }
        }
        advanceTimeBy(OFFLINE_WAIT * CONCURRENT_FINISHES)
        runCurrent()

        assertEquals(1, remote.finished.size)
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
        val expected = KronoResult.Success(FinishedSession(session(taskId = "tarea-1"), addedToTask = true))
        results.forEach { assertEquals(expected, it.await()) }
    }

    @Test
    fun finalizarDeNuevoLaMismaSesion_devuelveElMismoResultadoSinSumarOtraVez() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        val first = repository.finishActiveTimer(session(taskId = "tarea-1"))

        val repeated = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertEquals(first, repeated)
        assertEquals(1, remote.finished.size)
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
    }

    @Test
    fun finalizarUnaSesionQueYaNoEsLaDelCronometroEnCurso_noLoBorraYDevuelveNoActiveTimer() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        val newerTimer = ActiveTimer("tarea-1", start + 1.minutes, start + 1.minutes, 0.minutes)
        remote.givenActiveTimer(user.id, ActiveTimerDto.toMap(newerTimer))

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertEquals(KronoResult.Error(TimerError.NoActiveTimer), result)
        assertTrue(remote.finished.isEmpty())
        assertNotNull(remote.timers[user.id])
    }

    @Test
    fun siNoSePuedeComprobarElCronometroAntesDeFinalizar_finalizaIgual() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        givenRunningTimer("tarea-1")
        remote.readFailure = firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertTrue((result as KronoResult.Success).data.addedToTask)
        assertEquals(1, remote.finished.size)
    }

    @Test
    fun sinRedYSinElCronometroEnCache_leerloDevuelveQueNoHayNinguno() = runTest {
        auth.user.value = user
        remote.readFailure = firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)

        assertEquals(KronoResult.Success(null), repository.getActiveTimer())
    }

    @Test
    fun unErrorDeLecturaQueNoEsDeConexion_seDevuelveComoError() = runTest {
        auth.user.value = user
        remote.readFailure = firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        assertTrue(repository.getActiveTimer() is KronoResult.Error)
    }

    @Test
    fun iniciarSinRed_arrancaElCronometro() = runTest {
        auth.user.value = user
        remote.readFailure = firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)
        remote.serverConfirmation = CompletableDeferred()
        val clock = object : Clock {
            override fun now(): Instant = start
        }
        val tasks = FirestoreTaskRepository(FakeTaskRemoteDataSource(remote), auth, remote)
        val startTimer = StartTimerUseCase(repository, tasks, FinishTimerUseCase(repository, clock), clock)

        val result = backgroundScope.async { startTimer("tarea-1") }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertEquals(KronoResult.Success(Unit), result.await())
        val saved = ActiveTimerDto.fromMap(remote.timers.getValue(user.id))
        assertEquals(ActiveTimer("tarea-1", start, start, Duration.ZERO), saved)
    }

    @Test
    fun unDocumentoIncompleto_seIgnora() {
        assertNull(ActiveTimerDto.fromMap(mapOf(ActiveTimerDto.FIELD_TASK_ID to "tarea-1")))
    }

    private fun timer(taskId: String) = ActiveTimer(taskId, start, start, 0.minutes)

    private fun givenRunningTimer(taskId: String) {
        remote.givenActiveTimer(user.id, ActiveTimerDto.toMap(timer(taskId)))
    }

    private fun session(taskId: String) = TimeSession(
        id = timer(taskId).sessionId,
        taskId = taskId,
        startedAt = start,
        endedAt = start + 30.minutes,
        duration = 25.minutes,
        type = TimeSessionType.CRONOMETRO,
    )

    private fun firestoreError(code: FirebaseFirestoreException.Code) = FirebaseFirestoreException(code.name, code)

    private companion object {
        /** Más que la espera máxima a la confirmación del servidor, como si no hubiera red. */
        val OFFLINE_WAIT = 3.seconds

        /** Toques casi simultáneos sobre Finalizar con los que QA reprodujo el doble incremento. */
        const val CONCURRENT_FINISHES = 6
    }
}
