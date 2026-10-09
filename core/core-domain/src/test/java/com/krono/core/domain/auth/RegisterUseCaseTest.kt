package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUseCaseTest {

    private val repository = FakeAuthRepository()
    private val register = RegisterUseCase(repository)

    @Test
    fun conAlgunCampoVacioDevuelveCamposVaciosSinLlamarAlRepositorio() = runTest {
        assertEquals(KronoResult.Error(AuthError.EmptyFields), register("", "krono2026!", "krono2026!"))
        assertEquals(KronoResult.Error(AuthError.EmptyFields), register("ana@correo.com", "", "krono2026!"))
        assertEquals(KronoResult.Error(AuthError.EmptyFields), register("ana@correo.com", "krono2026!", ""))
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun conCorreoMalFormadoDevuelveCorreoInvalido() = runTest {
        assertEquals(
            KronoResult.Error(AuthError.InvalidEmail),
            register("anacorreo.com", "krono2026!", "krono2026!"),
        )
    }

    @Test
    fun conContrasenaDebilDevuelveLosRequisitosIncumplidos() = runTest {
        val resultado = register("ana@correo.com", "krono", "krono")

        assertEquals(
            KronoResult.Error(AuthError.WeakPassword(PasswordPolicy.evaluate("krono"))),
            resultado,
        )
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun conConfirmacionDistintaDevuelveContrasenasNoCoinciden() = runTest {
        assertEquals(
            KronoResult.Error(AuthError.PasswordsDoNotMatch),
            register("ana@correo.com", "krono2026!", "krono2026?"),
        )
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun conDatosValidosRegistraConElCorreoNormalizadoEIniciaSesion() = runTest {
        val resultado = register(" Ana@Correo.com", "krono2026!", "krono2026!")

        assertTrue(resultado is KronoResult.Success<*>)
        assertEquals("ana@correo.com", repository.lastEmailReceived)
        assertEquals("ana@correo.com", repository.currentSession?.email)
    }

    @Test
    fun conCorreoYaRegistradoPropagaElError() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")

        assertEquals(
            KronoResult.Error(AuthError.EmailAlreadyRegistered),
            register("ana@correo.com", "nueva2026!", "nueva2026!"),
        )
    }

    @Test
    fun sinConexionPropagaElErrorDeConexion() = runTest {
        repository.failure = AuthError.NoConnection

        assertEquals(
            KronoResult.Error(AuthError.NoConnection),
            register("ana@correo.com", "krono2026!", "krono2026!"),
        )
    }

    @Test
    fun unaFallaInesperadaSeConvierteEnErrorInesperado() = runTest {
        val causa = RuntimeException("INTERNAL_ERROR")
        repository.failure = causa

        assertEquals(
            KronoResult.Error(AuthError.Unexpected(causa)),
            register("ana@correo.com", "krono2026!", "krono2026!"),
        )
    }
}
