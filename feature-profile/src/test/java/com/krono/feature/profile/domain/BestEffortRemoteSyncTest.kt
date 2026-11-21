package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Pruebas unitarias para [syncRemoteBestEffort], verificando trazabilidad, captura de errores
 * técnicos remotos y propagación obligatoria de [CancellationException].
 */
class BestEffortRemoteSyncTest {

    @Test
    fun syncRemoteBestEffort_cuandoLlamadaEsExitosa_retornaSuccess() = runTest {
        var executed = false
        val result = syncRemoteBestEffort("operacion exitosa") {
            executed = true
            KronoResult.Success(Unit)
        }

        assertTrue(executed)
        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun syncRemoteBestEffort_cuandoLlamadaRetornaError_retornaError() = runTest {
        val expectedError = IllegalStateException("PERMISSION_DENIED")
        val result = syncRemoteBestEffort("operacion con error") {
            KronoResult.Error(expectedError)
        }

        assertTrue(result is KronoResult.Error)
        assertEquals(expectedError, (result as KronoResult.Error).throwable)
    }

    @Test
    fun syncRemoteBestEffort_cuandoLlamadaLanzaExcepcion_capturaYRetornaError() = runTest {
        val expectedException = RuntimeException("Error inesperado en Firestore")
        val result = syncRemoteBestEffort("operacion con excepcion") {
            throw expectedException
        }

        assertTrue(result is KronoResult.Error)
        assertEquals(expectedException, (result as KronoResult.Error).throwable)
    }

    @Test
    fun syncRemoteBestEffort_cuandoLlamadaEsCancelada_propagaCancellationException() = runTest {
        try {
            syncRemoteBestEffort("operacion cancelada") {
                throw CancellationException("Cancelado por corrutina padre")
            }
            fail("Debió propagar CancellationException")
        } catch (cancellation: CancellationException) {
            assertEquals("Cancelado por corrutina padre", cancellation.message)
        }
    }
}
