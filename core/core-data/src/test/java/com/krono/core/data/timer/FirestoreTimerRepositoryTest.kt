package com.krono.core.data.timer

import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishedSession
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de [FirestoreTimerRepository] y de la serialización del cronómetro (F-08): errores
 * locales, escrituras sin confirmación del servidor y sesiones de tareas borradas.
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

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        val finished = remote.finished.single()
        assertTrue(finished.addToTask)
        assertEquals(25.minutes.inWholeMilliseconds, finished.durationMillis)
        assertEquals("CRONOMETRO", finished.session[TimeSessionDto.FIELD_TYPE])
        assertEquals(KronoResult.Success(FinishedSession(session(taskId = "tarea-1"), addedToTask = true)), result)
    }

    @Test
    fun siLaTareaSeElimino_guardaLaSesionSinSumar() = runTest {
        auth.user.value = user

        val result = repository.finishActiveTimer(session(taskId = "borrada"))

        assertFalse(remote.finished.single().addToTask)
        assertEquals(KronoResult.Success(FinishedSession(session(taskId = "borrada"), addedToTask = false)), result)
    }

    @Test
    fun siLaTareaSeBorraEntreLaLecturaYElLote_conservaLaSesionSinSumarla() = runTest {
        auth.user.value = user
        remote.tasksDeletedAfterRead += "tarea-1"
        remote.givenActiveTimer(user.id, ActiveTimerDto.toMap(ActiveTimer("tarea-1", start, start, 0.minutes)))

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertEquals(KronoResult.Success(FinishedSession(session(taskId = "tarea-1"), addedToTask = false)), result)
        assertEquals(listOf("sesion-1"), remote.sessionsById.keys.toList())
        assertTrue(remote.incrementsByTask.isEmpty())
        assertNull(remote.timers[user.id])
    }

    @Test
    fun siNoSePuedeLeerLaTarea_finalizaSumandoleElTiempo() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        remote.taskExistsFailure = firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)

        val result = repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertTrue((result as KronoResult.Success).data.addedToTask)
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
    }

    @Test
    fun siElLoteFallaLocalmente_devuelveErrorYConservaElCronometro() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        remote.givenActiveTimer(user.id, ActiveTimerDto.toMap(ActiveTimer("tarea-1", start, start, 0.minutes)))
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
        remote.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async { repository.finishActiveTimer(session(taskId = "tarea-1")) }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertTrue(result.await() is KronoResult.Success)
        assertEquals(listOf("sesion-1"), remote.sessionsById.keys.toList())
    }

    @Test
    fun alReintentarTrasUnError_noDuplicaLaSesion() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"
        remote.writeFailure = firestoreError(FirebaseFirestoreException.Code.INVALID_ARGUMENT)
        val failed = repository.finishActiveTimer(session(taskId = "tarea-1"))
        remote.writeFailure = null

        repository.finishActiveTimer(session(taskId = "tarea-1"))

        assertTrue(failed is KronoResult.Error)
        assertEquals(listOf("sesion-1"), remote.sessionsById.keys.toList())
        assertEquals(25.minutes.inWholeMilliseconds, remote.incrementsByTask["tarea-1"])
    }

    @Test
    fun unDocumentoIncompleto_seIgnora() {
        assertNull(ActiveTimerDto.fromMap(mapOf(ActiveTimerDto.FIELD_TASK_ID to "tarea-1")))
    }

    private fun session(taskId: String) = TimeSession(
        id = "sesion-1",
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
    }
}
