package com.krono.core.data.auth

import app.cash.turbine.test
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseAuthRepositoryTest {

    private val dataSource = FakeAuthRemoteDataSource()
    private val repository = FirebaseAuthRepository(dataSource)

    @Test
    fun iniciarSesionDevuelveElUsuarioDeFirebase() = runTest {
        val resultado = repository.login("ana@correo.com", "krono2026!")

        assertEquals(KronoResult.Success(User(id = "uid-ana@correo.com", email = "ana@correo.com")), resultado)
    }

    @Test
    fun iniciarSesionConCredencialesRechazadasDevuelveCredencialesIncorrectas() = runTest {
        dataSource.failure = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "mal")

        assertEquals(KronoResult.Error(AuthError.InvalidCredentials), repository.login("ana@correo.com", "otra2026!"))
    }

    @Test
    fun crearCuentaIniciaLaSesionDelUsuarioNuevo() = runTest {
        val resultado = repository.register("ana@correo.com", "krono2026!")

        assertEquals("ana@correo.com", (resultado as KronoResult.Success<User>).data.email)
        assertEquals("ana@correo.com", dataSource.user.value?.email)
    }

    @Test
    fun crearCuentaConUnCorreoEnUsoDevuelveCorreoYaRegistrado() = runTest {
        dataSource.failure = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "en uso")

        assertEquals(
            KronoResult.Error(AuthError.EmailAlreadyRegistered),
            repository.register("ana@correo.com", "krono2026!"),
        )
    }

    @Test
    fun recuperarContrasenaEnviaElCorreoConFirebase() = runTest {
        assertEquals(KronoResult.Success(Unit), repository.requestPasswordReset("ana@correo.com"))
        assertEquals(listOf("ana@correo.com"), dataSource.resetEmailsSent)
    }

    @Test
    fun recuperarContrasenaDeUnCorreoSinCuentaDevuelveExitoNeutro() = runTest {
        // Sin la protección contra enumeración, Firebase avisa que el usuario no existe.
        dataSource.failure = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe")

        assertEquals(KronoResult.Success(Unit), repository.requestPasswordReset("nadie@correo.com"))
    }

    @Test
    fun recuperarContrasenaSinConexionDevuelveSinConexion() = runTest {
        dataSource.failure = FirebaseNetworkException("red")

        assertEquals(KronoResult.Error(AuthError.NoConnection), repository.requestPasswordReset("ana@correo.com"))
    }

    @Test
    fun cerrarSesionTerminaLaSesionDeFirebase() = runTest {
        repository.login("ana@correo.com", "krono2026!")

        assertEquals(KronoResult.Success(Unit), repository.logout())
        assertNull(dataSource.user.value)
    }

    @Test
    fun unaFallaAlCerrarSesionSeDevuelveComoErrorInesperadoSinLanzar() = runTest {
        val causa = IllegalStateException("almacenamiento")
        dataSource.failure = causa

        assertEquals(KronoResult.Error(AuthError.Unexpected(causa)), repository.logout())
    }

    @Test
    fun observarSesionEmiteElUsuarioActivoYNuloTrasCerrarSesion() = runTest {
        repository.observeSession().test {
            assertNull(awaitItem())

            repository.login("ana@correo.com", "krono2026!")
            assertEquals("ana@correo.com", awaitItem()?.email)

            repository.logout()
            assertNull(awaitItem())
        }
    }

    @Test
    fun siLaLecturaDeLaSesionFallaSeTrataComoSinSesion() = runTest {
        val fuenteQueFalla = object : AuthRemoteDataSource by dataSource {
            override fun observeUser(): Flow<User?> = flow { throw IllegalStateException("sin Firebase") }
        }

        FirebaseAuthRepository(fuenteQueFalla).observeSession().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
