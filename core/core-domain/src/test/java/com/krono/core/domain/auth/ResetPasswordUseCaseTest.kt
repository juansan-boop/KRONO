package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResetPasswordUseCaseTest {

    private val repository = FakeAuthRepository()
    private val resetPassword = ResetPasswordUseCase(repository)

    @Test
    fun conCorreoVacioDevuelveCamposVacios() = runTest {
        assertEquals(KronoResult.Error(AuthError.EmptyFields), resetPassword("  "))
        assertEquals(0, repository.resetCalls)
    }

    @Test
    fun conCorreoMalFormadoDevuelveCorreoInvalido() = runTest {
        assertEquals(KronoResult.Error(AuthError.InvalidEmail), resetPassword("ana@"))
        assertEquals(0, repository.resetCalls)
    }

    @Test
    fun conCorreoNoRegistradoDevuelveExitoParaNoRevelarQueCorreosTienenCuenta() = runTest {
        assertEquals(KronoResult.Success(Unit), resetPassword("nadie@correo.com"))
        assertEquals(1, repository.resetCalls)
    }

    @Test
    fun sinConexionDevuelveElErrorDeConexionDelRepositorio() = runTest {
        repository.failure = AuthError.NoConnection

        assertEquals(KronoResult.Error(AuthError.NoConnection), resetPassword("ana@correo.com"))
    }

    @Test
    fun conCorreoRegistradoDevuelveExitoUsandoElCorreoNormalizado() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")

        assertEquals(KronoResult.Success(Unit), resetPassword(" ANA@correo.com "))
        assertEquals("ana@correo.com", repository.lastEmailReceived)
    }

    @Test
    fun unaFallaInesperadaSeConvierteEnErrorInesperado() = runTest {
        val causa = RuntimeException("io")
        repository.failure = causa

        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), resetPassword("ana@correo.com"))
    }
}
