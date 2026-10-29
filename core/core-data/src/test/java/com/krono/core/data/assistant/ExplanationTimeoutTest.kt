package com.krono.core.data.assistant

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Pruebas del tiempo límite y del manejo de fallos de Gemini en el asistente (F-12). */
class ExplanationTimeoutTest {

    private val generator = FirebaseAiPriorityExplanationGenerator(PriorityPromptBuilder())

    @Test
    fun unaRespuestaLentaPeroDentroDelLimite_seAcepta() = runTest {
        // Las mediciones reales de Gemini fueron 5.0, 9.1 y 12.5 s: la última no cabía en 10 s.
        val result = generator.explainWith(plan()) {
            delay(12.5.seconds)
            "Empieza ya."
        }

        assertEquals(KronoResult.Success("Empieza ya."), result)
    }

    @Test
    fun unaRespuestaMasLentaQueElLimite_devuelveNoDisponible() = runTest {
        val result = generator.explainWith(plan()) {
            delay(FirebaseAiPriorityExplanationGenerator.TIMEOUT_SECONDS.seconds + 1.seconds)
            "Tarde."
        }

        assertTrue((result as KronoResult.Error).throwable is AssistantError.Unavailable)
    }

    @Test
    fun elLimiteDeEsperaEsDeVeinteSegundos() {
        assertEquals(20, FirebaseAiPriorityExplanationGenerator.TIMEOUT_SECONDS)
    }

    @Test
    fun unFalloDelModelo_devuelveNoDisponibleConLaCausa() = runTest {
        val cause = IllegalStateException("API deshabilitada")

        val result = generator.explainWith(plan()) { throw cause }

        val error = (result as KronoResult.Error).throwable as AssistantError.Unavailable
        // Compara tipo y mensaje: la recuperación de trazas de corrutinas puede copiar la excepción.
        assertEquals(cause.message, error.cause?.message)
        assertTrue(error.cause is IllegalStateException)
    }

    @Test
    fun laCancelacionDeLaCorrutina_seRelanza() = runTest {
        try {
            generator.explainWith(plan()) { throw CancellationException("cancelada") }
            fail("Debía relanzar la cancelación")
        } catch (expected: CancellationException) {
            assertEquals("cancelada", expected.message)
        }
    }

    private fun plan(): PriorityPlan {
        val planned = PlannedTask(
            task = Task(id = "1", title = "Tarea", category = TaskCategory.ESTUDIO),
            score = 1,
            factors = emptySet(),
            estimate = 30.minutes,
            estimateSource = EstimateSource.MANUAL,
            start = LocalDateTime(2026, 10, 22, 14, 0),
            end = LocalDateTime(2026, 10, 22, 14, 30),
            timeUntilDue = null,
        )
        return PriorityPlan(planned, emptyList(), 50, emptyMap())
    }
}
