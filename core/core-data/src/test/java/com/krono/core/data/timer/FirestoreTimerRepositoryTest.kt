package com.krono.core.data.timer

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [FirestoreTimerRepository] y de la serialización del cronómetro (F-08). */
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
    fun alFinalizar_sumaALaTareaSiTodaviaExiste() = runTest {
        auth.user.value = user
        remote.existingTasks += "tarea-1"

        repository.finishActiveTimer(session(taskId = "tarea-1"))

        val finished = remote.finished.single()
        assertTrue(finished.addToTask)
        assertEquals(25.minutes.inWholeMilliseconds, finished.durationMillis)
        assertEquals("CRONOMETRO", finished.session[TimeSessionDto.FIELD_TYPE])
    }

    @Test
    fun siLaTareaSeElimino_guardaLaSesionSinSumar() = runTest {
        auth.user.value = user

        repository.finishActiveTimer(session(taskId = "borrada"))

        assertFalse(remote.finished.single().addToTask)
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

    /** Fuente remota en memoria que registra lo que se escribiría en Firestore. */
    private class FakeTimerRemoteDataSource : TimerRemoteDataSource {
        data class Finished(val session: Map<String, Any?>, val durationMillis: Long, val addToTask: Boolean)

        val timers = mutableMapOf<String, Map<String, Any?>>()
        val existingTasks = mutableSetOf<String>()
        val finished = mutableListOf<Finished>()
        private val timerFlow = MutableStateFlow<Map<String, Any?>?>(null)

        override fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?> = timerFlow

        override suspend fun getActiveTimer(userId: String): Map<String, Any?>? = timers[userId]

        override fun saveActiveTimer(userId: String, timer: Map<String, Any?>) {
            timers[userId] = timer
            timerFlow.value = timer
        }

        override suspend fun taskExists(userId: String, taskId: String): Boolean = taskId in existingTasks

        override fun finishActiveTimer(
            userId: String,
            session: Map<String, Any?>,
            taskId: String,
            durationMillis: Long,
            addToTask: Boolean,
        ) {
            finished += Finished(session, durationMillis, addToTask)
            timers.remove(userId)
            timerFlow.value = null
        }
    }
}
