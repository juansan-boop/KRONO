package com.krono.feature.auth

import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.PasswordPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMessageTest {

    @Test
    fun cadaErrorDeDominioTieneSuMensajeDeUi() {
        assertEquals(AuthMessage.EmptyFields, AuthError.EmptyFields.toAuthMessage())
        assertEquals(AuthMessage.InvalidEmail, AuthError.InvalidEmail.toAuthMessage())
        assertEquals(AuthMessage.WeakPassword, AuthError.WeakPassword(PasswordPolicy.evaluate("")).toAuthMessage())
        assertEquals(AuthMessage.PasswordsDoNotMatch, AuthError.PasswordsDoNotMatch.toAuthMessage())
        assertEquals(AuthMessage.InvalidCredentials, AuthError.InvalidCredentials.toAuthMessage())
        assertEquals(AuthMessage.EmailAlreadyRegistered, AuthError.EmailAlreadyRegistered.toAuthMessage())
        assertEquals(AuthMessage.NoConnection, AuthError.NoConnection.toAuthMessage())
        assertEquals(AuthMessage.TooManyRequests, AuthError.TooManyRequests.toAuthMessage())
        assertEquals(AuthMessage.Unexpected, AuthError.Unexpected(RuntimeException()).toAuthMessage())
    }

    @Test
    fun unaExcepcionQueNoEsDeDominioSeMuestraComoErrorInesperado() {
        assertEquals(AuthMessage.Unexpected, IllegalStateException("INTERNAL_ERROR").toAuthMessage())
    }
}
