package com.krono.core.data.profile

import app.cash.turbine.test
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Pruebas de [FirestoreUserNameRepository] (F-13 CA-1: nombre del saludo). */
class FirestoreUserNameRepositoryTest {

    private lateinit var auth: FakeAuthRemoteDataSource
    private lateinit var remote: FakeUserNameRemoteDataSource
    private lateinit var repository: FirestoreUserNameRepository

    @Before
    fun setUp() {
        auth = FakeAuthRemoteDataSource()
        remote = FakeUserNameRemoteDataSource()
        repository = FirestoreUserNameRepository(remote, auth)
    }

    @Test
    fun sinSesion_emiteNulo() = runTest {
        repository.observeUserName().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun conSesion_emiteElNombreDelPerfilYSusCambios() = runTest {
        auth.user.value = User(id = "u1", email = "a@krono.app")
        remote.names["u1"] = MutableStateFlow("Alexa")

        repository.observeUserName().test {
            assertEquals("Alexa", awaitItem())
            remote.names.getValue("u1").value = "Alex"
            assertEquals("Alex", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun siFallaLaLectura_emiteNuloEnVezDeFallar() = runTest {
        auth.user.value = User(id = "u1", email = "a@krono.app")
        remote.failure = IllegalStateException("sin permiso")

        repository.observeUserName().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}

/** Doble de [UserNameRemoteDataSource]: un flujo de nombre por usuario. */
private class FakeUserNameRemoteDataSource : UserNameRemoteDataSource {

    val names = mutableMapOf<String, MutableStateFlow<String?>>()
    var failure: Throwable? = null

    override fun observeUserName(userId: String): Flow<String?> {
        val error = failure ?: return names.getValue(userId)
        return flow { throw error }
    }
}
