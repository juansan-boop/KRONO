package com.krono.core.data.timer

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [FirestoreTimeSessionRepository] y de la lectura de [TimeSessionDto] (F-13 CA-1). */
class FirestoreTimeSessionRepositoryTest {

    private val user = User(id = "user-1", email = "estudiante@krono.app")
    private val from = Instant.parse("2026-10-30T00:00:00Z")
    private val to = Instant.parse("2026-10-31T00:00:00Z")
    private val session = TimeSession(
        id = "t_1",
        taskId = "t",
        startedAt = Instant.parse("2026-10-30T09:00:00Z"),
        endedAt = Instant.parse("2026-10-30T09:40:00Z"),
        duration = 40.minutes,
        type = TimeSessionType.ENFOQUE,
    )

    private lateinit var auth: FakeAuthRemoteDataSource
    private lateinit var remote: FakeTimeSessionRemoteDataSource
    private lateinit var repository: FirestoreTimeSessionRepository

    @Before
    fun setUp() {
        auth = FakeAuthRemoteDataSource()
        remote = FakeTimeSessionRemoteDataSource()
        repository = FirestoreTimeSessionRepository(remote, auth)
    }

    @Test
    fun documentoDeSesion_seConvierteDeIdaYVuelta() {
        assertEquals(session, TimeSessionDto.fromMap(TimeSessionDto.toMap(session)))
    }

    @Test
    fun documentoSinCamposObligatorios_seDescarta() {
        assertNull(TimeSessionDto.fromMap(mapOf(TimeSessionDto.FIELD_ID to "x")))
    }

    @Test
    fun sinSesion_emiteListaVaciaSinConsultarFirestore() = runTest {
        repository.observeSessionsBetween(from, to).test {
            assertEquals(KronoResult.Success(emptyList<TimeSession>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(remote.requests.isEmpty())
    }

    @Test
    fun conSesion_consultaElIntervaloDelUsuarioYMapeaLasSesiones() = runTest {
        auth.user.value = user
        remote.documents.value = listOf(TimeSessionDto.toMap(session), mapOf("basura" to 1))

        repository.observeSessionsBetween(from, to).test {
            assertEquals(KronoResult.Success(listOf(session)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            listOf(Triple("user-1", from.toEpochMilliseconds(), to.toEpochMilliseconds())),
            remote.requests,
        )
    }

    @Test
    fun siFallaElListener_emiteErrorYTermina() = runTest {
        auth.user.value = user
        remote.failure = IllegalStateException("permiso denegado")

        repository.observeSessionsBetween(from, to).test {
            assertTrue(awaitItem() is KronoResult.Error)
            awaitComplete()
        }
    }
}

/** Doble de [TimeSessionRemoteDataSource]: emite [documents] y registra cada consulta. */
private class FakeTimeSessionRemoteDataSource : TimeSessionRemoteDataSource {

    val documents = MutableStateFlow<List<Map<String, Any?>>>(emptyList())
    var failure: Throwable? = null
    val requests = mutableListOf<Triple<String, Long, Long>>()

    override fun observeSessionsBetween(
        userId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<List<Map<String, Any?>>> {
        requests += Triple(userId, fromMillis, toMillis)
        val error = failure ?: return documents
        return flow { throw error }
    }
}
