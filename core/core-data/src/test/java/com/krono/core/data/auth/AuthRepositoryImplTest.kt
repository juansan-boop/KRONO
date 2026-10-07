package com.krono.core.data.auth

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.User
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeAuthDao()
    private val hasher = Pbkdf2PasswordHasher(iterations = 1_000)
    private val repository = AuthRepositoryImpl(dao, hasher, dispatcher)

    @Test
    fun registrarGuardaElUsuarioConHashYSalSinLaContrasenaEnTextoPlano() = runTest(dispatcher) {
        val resultado = repository.register("ana@correo.com", "krono2026!")

        assertTrue(resultado is KronoResult.Success<*>)
        val guardado = dao.storedUsers.single()
        assertEquals("ana@correo.com", guardado.email)
        assertFalse(guardado.passwordHash.contains("krono2026!"))
        assertTrue(guardado.passwordSalt.isNotBlank())
        assertTrue(hasher.verify("krono2026!", guardado.toHashedPassword()))
    }

    @Test
    fun registrarIniciaSesionConElUsuarioCreado() = runTest(dispatcher) {
        val usuario = (repository.register("ana@correo.com", "krono2026!") as KronoResult.Success<User>).data

        assertEquals(usuario.id, dao.storedSession?.userId)
    }

    @Test
    fun registrarUnCorreoExistenteDevuelveCorreoYaRegistrado() = runTest(dispatcher) {
        repository.register("ana@correo.com", "krono2026!")

        val resultado = repository.register("ana@correo.com", "otra2026!")

        assertEquals(KronoResult.Error(AuthError.EmailAlreadyRegistered), resultado)
        assertEquals(1, dao.storedUsers.size)
    }

    @Test
    fun loginConCredencialesCorrectasIniciaSesion() = runTest(dispatcher) {
        repository.register("ana@correo.com", "krono2026!")
        repository.logout()

        val resultado = repository.login("ana@correo.com", "krono2026!")

        assertEquals("ana@correo.com", (resultado as KronoResult.Success<User>).data.email)
        assertNotNull(dao.storedSession)
    }

    @Test
    fun loginConContrasenaIncorrectaDevuelveCredencialesInvalidasSinIniciarSesion() = runTest(dispatcher) {
        repository.register("ana@correo.com", "krono2026!")
        repository.logout()

        assertEquals(KronoResult.Error(AuthError.InvalidCredentials), repository.login("ana@correo.com", "mala2026!"))
        assertNull(dao.storedSession)
    }

    @Test
    fun loginConCorreoInexistenteDevuelveCredencialesInvalidas() = runTest(dispatcher) {
        assertEquals(KronoResult.Error(AuthError.InvalidCredentials), repository.login("nadie@correo.com", "krono2026!"))
    }

    @Test
    fun recuperarConCorreoRegistradoEsExitosoYConNoRegistradoFalla() = runTest(dispatcher) {
        repository.register("ana@correo.com", "krono2026!")

        assertEquals(KronoResult.Success(Unit), repository.requestPasswordReset("ana@correo.com"))
        assertEquals(
            KronoResult.Error(AuthError.EmailNotRegistered),
            repository.requestPasswordReset("nadie@correo.com"),
        )
    }

    @Test
    fun observarSesionEmiteElUsuarioActivoYNuloTrasCerrarSesion() = runTest(dispatcher) {
        repository.observeSession().test {
            assertNull(awaitItem())

            repository.register("ana@correo.com", "krono2026!")
            assertEquals("ana@correo.com", awaitItem()?.email)

            assertEquals(KronoResult.Success(Unit), repository.logout())
            assertNull(awaitItem())
        }
    }

    @Test
    fun unaFallaDeLaBaseDeDatosSeDevuelveComoErrorInesperadoSinLanzar() = runTest(dispatcher) {
        val causa = IllegalStateException("database is locked")
        dao.failure = causa

        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), repository.login("ana@correo.com", "krono2026!"))
        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), repository.register("ana@correo.com", "krono2026!"))
        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), repository.requestPasswordReset("ana@correo.com"))
        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), repository.logout())
    }
}
