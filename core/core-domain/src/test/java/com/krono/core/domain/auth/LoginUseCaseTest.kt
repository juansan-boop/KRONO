package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val repository = FakeAuthRepository()
    private val login = LoginUseCase(repository)

    @Test
    fun conCorreoVacioDevuelveCamposVaciosSinLlamarAlRepositorio() = runTest {
        val resultado = login(email = " ", password = "krono2026!")

        assertEquals(KronoResult.Error(AuthError.EmptyFields), resultado)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun conContrasenaVaciaDevuelveCamposVacios() = runTest {
        assertEquals(KronoResult.Error(AuthError.EmptyFields), login(email = "ana@correo.com", password = ""))
    }

    @Test
    fun conCorreoMalFormadoDevuelveCorreoInvalido() = runTest {
        val resultado = login(email = "ana@correo", password = "krono2026!")

        assertEquals(KronoResult.Error(AuthError.InvalidEmail), resultado)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun conCredencialesCorrectasDevuelveElUsuario() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")

        val resultado = login(email = "ana@correo.com", password = "krono2026!")

        assertTrue(resultado is KronoResult.Success<*>)
        assertEquals("ana@correo.com", (resultado as KronoResult.Success<User>).data.email)
    }

    @Test
    fun normalizaElCorreoAntesDeConsultarElRepositorio() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")

        val resultado = login(email = "  Ana@Correo.com ", password = "krono2026!")

        assertEquals("ana@correo.com", repository.lastEmailReceived)
        assertTrue(resultado is KronoResult.Success<*>)
    }

    @Test
    fun conCredencialesIncorrectasPropagaElErrorDelRepositorio() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")

        assertEquals(
            KronoResult.Error(AuthError.InvalidCredentials),
            login(email = "ana@correo.com", password = "otra2026!"),
        )
    }

    @Test
    fun demasiadosIntentosLlegaComoErrorDeDominioSinEnvolver() = runTest {
        repository.failure = AuthError.TooManyRequests

        assertEquals(
            KronoResult.Error(AuthError.TooManyRequests),
            login(email = "ana@correo.com", password = "krono2026!"),
        )
    }

    @Test
    fun unaFallaInesperadaDelRepositorioSeConvierteEnErrorInesperado() = runTest {
        val causa = IllegalStateException("disco lleno")
        repository.failure = causa

        assertEquals(
            KronoResult.Error(AuthError.Unexpected(causa)),
            login(email = "ana@correo.com", password = "krono2026!"),
        )
    }
}
