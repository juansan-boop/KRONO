package com.krono.core.data.firestore

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
 * Pruebas unitarias de confirmación de escritura en Firestore ([awaitWriteConfirmation] y [awaitConfirmationOrQueued]).
 *
 * Valida la diferenciación explícita entre confirmación remota ([WriteConfirmationResult.Confirmed])
 * y escrituras encoladas en caché local por tiempo de espera agotado ([WriteConfirmationResult.Queued]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WriteConfirmationTest {

    @Test
    fun awaitWriteConfirmation_cuandoEscrituraConfirmaDentroDelLimite_retornaConfirmed() = runTest {
        var executed = false
        val result = awaitWriteConfirmation(timeout = TEST_TIMEOUT) {
            executed = true
        }

        assertTrue(executed)
        assertEquals(WriteConfirmationResult.Confirmed, result)
    }

    @Test
    fun awaitWriteConfirmation_cuandoEscrituraNoConfirmaDentroDelLimite_retornaQueued() = runTest {
        val pendingConfirmation = CompletableDeferred<Unit>()
        var result: WriteConfirmationResult? = null

        val job = launch {
            result = awaitWriteConfirmation(timeout = TEST_TIMEOUT) {
                pendingConfirmation.await()
            }
        }

        runCurrent()
        assertNullResult(result)

        advanceTimeBy(TEST_TIMEOUT.inWholeMilliseconds + 1)
        runCurrent()

        assertTrue(job.isCompleted)
        assertEquals(WriteConfirmationResult.Queued, result)
    }

    @Test
    fun awaitWriteConfirmation_cuandoEscrituraLanzaExcepcion_propagaExcepcion() = runTest {
        val expectedException = IllegalStateException("PERMISSION_DENIED_TEST")

        try {
            awaitWriteConfirmation(timeout = TEST_TIMEOUT) {
                throw expectedException
            }
            fail("Debió propagar la excepción remota")
        } catch (actual: IllegalStateException) {
            assertEquals("PERMISSION_DENIED_TEST", actual.message)
        }
    }

    @Test
    fun awaitWriteConfirmation_cuandoEscrituraEsCancelada_propagaCancellationException() = runTest {
        try {
            awaitWriteConfirmation(timeout = TEST_TIMEOUT) {
                throw CancellationException("Operación cancelada")
            }
            fail("Debió propagar CancellationException")
        } catch (cancellation: CancellationException) {
            assertEquals("Operación cancelada", cancellation.message)
        }
    }

    @Test
    fun awaitConfirmationOrQueued_delegaEnAwaitWriteConfirmation_confirmandoEscritura() = runTest {
        var called = false
        val result = awaitConfirmationOrQueued {
            called = true
        }

        assertTrue(called)
        assertEquals(WriteConfirmationResult.Confirmed, result)
    }

    private fun assertNullResult(result: WriteConfirmationResult?) {
        assertFalse("El resultado no debe estar listo antes de cumplir timeout", result != null)
    }

    private companion object {
        val TEST_TIMEOUT = 2.seconds
    }
}
