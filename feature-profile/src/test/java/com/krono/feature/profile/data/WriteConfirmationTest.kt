package com.krono.feature.profile.data

import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de [awaitWriteOrQueued] y [ignoringNetworkFailure]: una escritura de perfil sin confirmación
 * del servidor no deja la operación colgada, pero los errores reales siguen llegando (F-15, F-19, F-20).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WriteConfirmationTest {

    @Test
    fun escrituraConfirmadaDevuelveSinError() = runTest {
        var confirmed = false

        awaitWriteOrQueued(TIMEOUT) { confirmed = true }

        assertTrue(confirmed)
    }

    @Test
    fun escrituraSinConfirmacionDelServidorTerminaAlVencerElLimiteSinError() = runTest {
        val neverConfirmed = CompletableDeferred<Unit>()
        var finished = false
        val job = launch {
            awaitWriteOrQueued(TIMEOUT) { neverConfirmed.await() }
            finished = true
        }

        runCurrent()
        assertFalse("Debe esperar hasta el límite", finished)
        advanceTimeBy(TIMEOUT.inWholeMilliseconds + 1)
        runCurrent()

        assertTrue("Al vencer el límite la escritura se da por encolada", finished)
        assertTrue(job.isCompleted)
    }

    @Test
    fun errorRealAntesDelLimiteSePropaga() = runTest {
        val rejection = IllegalStateException("PERMISSION_DENIED")

        try {
            awaitWriteOrQueued(TIMEOUT) { throw rejection }
            fail("Debía propagar el rechazo")
        } catch (error: IllegalStateException) {
            assertEquals(rejection.message, error.message)
        }
    }

    @Test
    fun ignoringNetworkFailureTragaErroresDeRed() = runTest {
        ignoringNetworkFailure { throw FirebaseNetworkException("sin red") }
    }

    @Test
    fun ignoringNetworkFailurePropagaErroresQueNoSonDeRed() = runTest {
        val rejection = IllegalStateException("otro")

        try {
            ignoringNetworkFailure { throw rejection }
            fail("Debía propagar el error")
        } catch (error: IllegalStateException) {
            assertEquals(rejection.message, error.message)
        }
    }

    @Test
    fun ignoringNetworkFailureNoTragaLaCancelacion() = runTest {
        try {
            ignoringNetworkFailure { throw CancellationException("cancelada") }
            fail("Debía propagar la cancelación")
        } catch (_: CancellationException) {
            // Esperado: la cancelación debe seguir su curso.
        }
    }

    private companion object {
        val TIMEOUT = 2.seconds
    }
}
