package com.krono.feature.profile.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.feature.profile.domain.InvalidPasswordException
import com.krono.feature.profile.domain.NetworkUnavailableException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [toProfileOperationError]: traducción de excepciones de Firebase a errores del dominio. */
class ProfileErrorMappingTest {

    @Test
    fun credencialInvalidaSeTraduceAInvalidPassword() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "mala")

        assertTrue(error.toProfileOperationError() is InvalidPasswordException)
    }

    @Test
    fun errorDeRedDeFirebaseSeTraduceANetworkUnavailable() {
        assertTrue(FirebaseNetworkException("sin red").toProfileOperationError() is NetworkUnavailableException)
    }

    @Test
    fun firestoreUnavailableSeTraduceANetworkUnavailable() {
        val error = FirebaseFirestoreException("offline", FirebaseFirestoreException.Code.UNAVAILABLE)

        assertTrue(error.toProfileOperationError() is NetworkUnavailableException)
    }

    @Test
    fun otroErrorSePropagaSinCambios() {
        val error = IllegalStateException("otro")

        assertSame(error, error.toProfileOperationError())
    }
}
