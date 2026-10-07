package com.krono.core.domain.auth

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionUseCasesTest {

    private val repository = FakeAuthRepository()
    private val observeSession = ObserveSessionUseCase(repository)
    private val logout = LogoutUseCase(repository)

    @Test
    fun sinSesionActivaEmiteNulo() = runTest {
        observeSession().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun conSesionActivaEmiteElUsuario() = runTest {
        val usuario = User(id = "1", email = "ana@correo.com")
        repository.givenActiveSession(usuario)

        observeSession().test {
            assertEquals(usuario, awaitItem())
        }
    }

    @Test
    fun cerrarSesionTerminaLaSesionActiva() = runTest {
        repository.givenActiveSession(User(id = "1", email = "ana@correo.com"))

        observeSession().test {
            assertEquals("ana@correo.com", awaitItem()?.email)

            assertEquals(KronoResult.Success(Unit), logout())

            assertNull(awaitItem())
        }
    }

    @Test
    fun unaFallaAlCerrarSesionSeConvierteEnErrorInesperado() = runTest {
        val causa = RuntimeException("io")
        repository.failure = causa

        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), logout())
    }
}
