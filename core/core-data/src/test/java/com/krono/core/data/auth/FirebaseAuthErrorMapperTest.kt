package com.krono.core.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.PasswordPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FirebaseAuthErrorMapperTest {

    @Test
    fun unaFallaDeRedSeTraduceASinConexion() {
        assertEquals(AuthError.NoConnection, FirebaseAuthErrorMapper.map(FirebaseNetworkException("red")))
    }

    @Test
    fun elBloqueoPorExcesoDeSolicitudesSeTraduceADemasiadosIntentos() {
        assertEquals(
            AuthError.TooManyRequests,
            FirebaseAuthErrorMapper.map(FirebaseTooManyRequestsException("bloqueado")),
        )
    }

    @Test
    fun unaCredencialInvalidaSeTraduceACredencialesIncorrectas() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "mal")

        assertEquals(AuthError.InvalidCredentials, FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unaContrasenaIncorrectaSeTraduceACredencialesIncorrectas() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "mal")

        assertEquals(AuthError.InvalidCredentials, FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unUsuarioInexistenteSeTraduceAlMismoMensajeDeCredencialesIncorrectas() {
        val error = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe")

        assertEquals(AuthError.InvalidCredentials, FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unCorreoMalFormadoSeTraduceACorreoInvalido() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "formato")

        assertEquals(AuthError.InvalidEmail, FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unCorreoEnUsoSeTraduceACorreoYaRegistrado() {
        val error = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "en uso")

        assertEquals(AuthError.EmailAlreadyRegistered, FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unaContrasenaDebilSeTraduceConLosRequisitosDeLaContrasenaIntentada() {
        val error = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "debil", "corta")

        assertEquals(
            AuthError.WeakPassword(PasswordPolicy.evaluate("abc")),
            FirebaseAuthErrorMapper.map(error, attemptedPassword = "abc"),
        )
    }

    @Test
    fun unCodigoDeFirebaseNoPrevistoSeTraduceAErrorInesperado() {
        val error = FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "deshabilitado")

        assertEquals(AuthError.Unexpected(error), FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unaExcepcionAjenaAFirebaseSeTraduceAErrorInesperado() {
        val error = IllegalStateException("boom")

        assertEquals(AuthError.Unexpected(error), FirebaseAuthErrorMapper.map(error))
    }

    @Test
    fun unErrorDeDominioSeConservaTalCual() {
        assertSame(AuthError.NoConnection, FirebaseAuthErrorMapper.map(AuthError.NoConnection))
    }
}
