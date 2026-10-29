package com.krono.feature.profile.data

import com.krono.feature.profile.domain.RemoteTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Pruebas de [withRemoteTimeout]: una escritura que nunca se confirma no deja la operación colgada. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTimeoutTest {

    @Test
    fun devuelveElResultadoSiLaOperacionTerminaAntesDelLimite() = runTest {
        val value = withRemoteTimeout(TIMEOUT) { "listo" }

        assertEquals("listo", value)
    }

    @Test
    fun lanzaRemoteTimeoutExceptionSiLaOperacionNuncaSeConfirma() = runTest {
        var failure: Throwable? = null
        val job = launch {
            try {
                withRemoteTimeout(TIMEOUT) { CompletableDeferred<Unit>().await() }
            } catch (error: RemoteTimeoutException) {
                failure = error
            }
        }

        advanceTimeBy(TIMEOUT.inWholeMilliseconds + 1)
        runCurrent()

        assertTrue(job.isCompleted)
        assertTrue(failure is RemoteTimeoutException)
    }

    private companion object {
        val TIMEOUT = 5.seconds
    }
}
