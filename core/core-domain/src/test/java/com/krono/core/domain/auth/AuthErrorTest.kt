package com.krono.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AuthErrorTest {

    @Test
    fun unAuthErrorSeConservaTalCual() {
        assertSame(AuthError.InvalidCredentials, AuthError.InvalidCredentials.asAuthError())
    }

    @Test
    fun cualquierOtraExcepcionSeEnvuelveComoErrorInesperado() {
        val causa = IllegalStateException("boom")

        assertEquals(AuthError.Unexpected(causa), causa.asAuthError())
    }
}
