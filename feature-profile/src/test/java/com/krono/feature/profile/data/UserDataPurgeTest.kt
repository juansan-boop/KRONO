package com.krono.feature.profile.data

import com.krono.feature.profile.domain.RemoteTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de la purga por lotes de F-21: partición de las tareas y orden de confirmación,
 * con el lote final (cronómetro y perfil) siempre al último.
 */
class UserDataPurgeTest {

    private val taskBatchSize = MAX_TASKS_PER_BATCH

    @Test
    fun splitIntoPurgeBatches_sinTareas_devuelveUnSoloLoteFinalVacio() {
        assertEquals(listOf(emptyList<Int>()), splitIntoPurgeBatches(emptyList<Int>()))
    }

    @Test
    fun splitIntoPurgeBatches_conUnaTarea_devuelveUnLoteConEsaTarea() {
        assertEquals(listOf(listOf(1)), splitIntoPurgeBatches(listOf(1)))
    }

    @Test
    fun splitIntoPurgeBatches_conExactamenteElTamanoDelLote_devuelveUnSoloLote() {
        val batches = splitIntoPurgeBatches(List(taskBatchSize) { it })

        assertEquals(listOf(taskBatchSize), batches.map { it.size })
    }

    @Test
    fun splitIntoPurgeBatches_conUnaTareaMasQueUnLote_dejaLaSobranteEnElLoteFinal() {
        val batches = splitIntoPurgeBatches(List(taskBatchSize + 1) { it })

        assertEquals(listOf(taskBatchSize, 1), batches.map { it.size })
    }

    @Test
    fun splitIntoPurgeBatches_conMilTareas_noExcedeElLimiteDeFirestoreEnNingunLote() {
        val batches = splitIntoPurgeBatches(List(1000) { it })

        assertEquals(listOf(taskBatchSize, taskBatchSize, 1000 - 2 * taskBatchSize), batches.map { it.size })
        assertEquals(1000, batches.sumOf { it.size })
        // El lote final suma el cronómetro y el perfil a sus tareas.
        assertTrue(batches.all { it.size + FINAL_BATCH_RESERVED_OPERATIONS <= MAX_BATCH_OPERATIONS })
    }

    @Test
    fun purgeInBatches_confirmaLosLotesPreviosEnOrdenYElFinalAlUltimo() = runTest {
        val events = mutableListOf<String>()

        purgeInBatches(
            readTaskRefs = { List(1000) { it } },
            commitTaskBatch = { events += "tareas:${it.size}" },
            commitFinalBatch = { events += "final:${it.size}" },
        )

        assertEquals(listOf("tareas:$taskBatchSize", "tareas:$taskBatchSize", "final:4"), events)
    }

    @Test
    fun purgeInBatches_sinTareas_confirmaSoloElLoteFinal() = runTest {
        val events = mutableListOf<String>()

        purgeInBatches(
            readTaskRefs = { emptyList<Int>() },
            commitTaskBatch = { events += "tareas:${it.size}" },
            commitFinalBatch = { events += "final:${it.size}" },
        )

        assertEquals(listOf("final:0"), events)
    }

    @Test
    fun purgeInBatches_siUnLotePrevioFalla_noEjecutaElLoteFinal() = runTest {
        var finalCommitted = false
        var taskBatchesCommitted = 0

        val failure = assertSuspendThrows<IllegalStateException> {
            purgeInBatches(
                readTaskRefs = { List(1000) { it } },
                commitTaskBatch = {
                    taskBatchesCommitted++
                    if (taskBatchesCommitted == 2) throw IllegalStateException("lote 2 rechazado")
                },
                commitFinalBatch = { finalCommitted = true },
            )
        }

        assertEquals("lote 2 rechazado", failure.message)
        assertFalse(finalCommitted)
    }

    @Test
    fun purgeInBatches_siLaLecturaFalla_noConfirmaNingunLote() = runTest {
        var commits = 0

        assertSuspendThrows<IllegalStateException> {
            purgeInBatches<Int>(
                readTaskRefs = { throw IllegalStateException("sin servidor") },
                commitTaskBatch = { commits++ },
                commitFinalBatch = { commits++ },
            )
        }

        assertEquals(0, commits)
    }

    @Test
    fun purgeInBatches_siLaLecturaNoResponde_lanzaTiempoAgotadoSinConfirmarNada() = runTest {
        var commits = 0

        assertSuspendThrows<RemoteTimeoutException> {
            purgeInBatches(
                readTaskRefs = { CompletableDeferred<List<Int>>().await() },
                commitTaskBatch = { commits++ },
                commitFinalBatch = { commits++ },
                timeout = STEP_TIMEOUT,
            )
        }

        assertEquals(0, commits)
    }

    @Test
    fun purgeInBatches_siUnLotePrevioSeAgotaEnTiempo_noEjecutaElLoteFinal() = runTest {
        var finalCommitted = false

        assertSuspendThrows<RemoteTimeoutException> {
            purgeInBatches(
                readTaskRefs = { List(taskBatchSize + 1) { it } },
                commitTaskBatch = { CompletableDeferred<Unit>().await() },
                commitFinalBatch = { finalCommitted = true },
                timeout = STEP_TIMEOUT,
            )
        }

        assertFalse(finalCommitted)
    }

    @Test
    fun purgeInBatches_siElLoteFinalSeAgotaEnTiempo_lanzaTiempoAgotado() = runTest {
        assertSuspendThrows<RemoteTimeoutException> {
            purgeInBatches(
                readTaskRefs = { listOf(1) },
                commitTaskBatch = { },
                commitFinalBatch = { CompletableDeferred<Unit>().await() },
                timeout = STEP_TIMEOUT,
            )
        }
    }

    @Test
    fun purgeInBatches_elLimiteEsPorLote_variosLotesLentosQueSumanMasDelLimiteNoExpiran() = runTest {
        val stepDuration = STEP_TIMEOUT * SLOW_STEP_FRACTION
        var finalCommitted = false

        purgeInBatches(
            readTaskRefs = { List(1000) { it } },
            commitTaskBatch = { delay(stepDuration) },
            commitFinalBatch = {
                delay(stepDuration)
                finalCommitted = true
            },
            timeout = STEP_TIMEOUT,
        )

        // Lectura rápida + 3 lotes de 0,6 x límite: el total (1,8 x límite) supera un límite global único.
        assertTrue(finalCommitted)
        assertTrue(testScheduler.currentTime > STEP_TIMEOUT.inWholeMilliseconds)
    }

    private suspend inline fun <reified E : Throwable> assertSuspendThrows(block: suspend () -> Unit): E {
        try {
            block()
        } catch (error: Throwable) {
            if (error is E) return error
            throw error
        }
        throw AssertionError("Se esperaba ${E::class.simpleName}")
    }

    private companion object {
        val STEP_TIMEOUT = 10.seconds
        const val SLOW_STEP_FRACTION = 0.6
    }
}
