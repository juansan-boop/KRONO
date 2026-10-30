package com.krono.core.data.firestore

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Pruebas de [readFromServerOrCache]: una lectura de comprobación nunca bloquea la acción del
 * usuario esperando al servidor (F-08, W-3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerOrCacheReadTest {

    @Test
    fun conRed_devuelveLoQueLeeDelServidor() = runTest {
        val result = readFromServerOrCache(fromServer = { "servidor" }, fromCache = { "caché" })

        assertEquals("servidor", result)
    }

    @Test
    fun sinRed_devuelveLoQueHayEnCache() = runTest {
        val result = readFromServerOrCache(
            fromServer = { throw unavailable() },
            fromCache = { "caché" },
        )

        assertEquals("caché", result)
    }

    @Test
    fun siElServidorNoRespondeATiempo_leeDeLaCacheSinEsperarMas() = runTest {
        val result = backgroundScope.async {
            readFromServerOrCache(fromServer = { awaitCancellation() }, fromCache = { "caché" })
        }

        advanceTimeBy(SERVER_READ_TIMEOUT - 1.milliseconds)
        runCurrent()
        assertFalse(result.isCompleted)
        advanceTimeBy(1.milliseconds)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals("caché", result.await())
    }

    @Test(expected = FirebaseFirestoreException::class)
    fun sinRedYSinElDocumentoEnCache_lanzaElErrorDeLaCache() = runTest {
        readFromServerOrCache<String>(fromServer = { throw unavailable() }, fromCache = { throw unavailable() })
    }

    @Test
    fun unErrorDelServidorQueNoEsDeConexion_seRespetaSinLeerLaCache() = runTest {
        var cacheRead = false

        val error = runCatching {
            readFromServerOrCache(
                fromServer = { throw FirebaseFirestoreException("denegado", FirebaseFirestoreException.Code.PERMISSION_DENIED) },
                fromCache = { cacheRead = true; "caché" },
            )
        }.exceptionOrNull()

        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (error as FirebaseFirestoreException).code)
        assertFalse(cacheRead)
    }

    private fun unavailable() =
        FirebaseFirestoreException("sin conexión", FirebaseFirestoreException.Code.UNAVAILABLE)
}
