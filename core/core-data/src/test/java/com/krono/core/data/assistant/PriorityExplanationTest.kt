package com.krono.core.data.assistant

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.PriorityFactor
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas del prompt y de la validación de respuestas del asistente (F-12). */
class PriorityExplanationTest {

    private val builder = PriorityPromptBuilder()

    @Test
    fun elPromptIncluyeLosDatosDeLaTareaYSusMotivos() {
        val prompt = builder.build(plan())

        assertTrue(prompt.contains("\"Entrega informe trimestral\""))
        assertTrue(prompt.contains("Estudio"))
        assertTrue(prompt.contains("22/10 18:00"))
        assertTrue(prompt.contains("vence en pocas horas"))
        assertTrue(prompt.contains("prioridad alta"))
        assertTrue(prompt.contains("\"Reunión de equipo\""))
    }

    @Test
    fun elPromptProhibeInventarDatosCircadianos() {
        assertTrue(builder.build(plan()).contains("No inventes datos biológicos"))
    }

    @Test
    fun unaRespuestaVaciaOMuyLarga_noEsValida() {
        val tooLong = "a".repeat(FirebaseAiPriorityExplanationGenerator.MAX_EXPLANATION_LENGTH + 1)

        listOf(null, "   ", tooLong).forEach { text ->
            val result = FirebaseAiPriorityExplanationGenerator.validate(text)
            assertEquals(KronoResult.Error(AssistantError.InvalidResponse), result)
        }
    }

    @Test
    fun unaRespuestaValida_seRecortaYSeAcepta() {
        val result = FirebaseAiPriorityExplanationGenerator.validate("  Empieza ya.  ")

        assertEquals(KronoResult.Success("Empieza ya."), result)
        assertFalse((result as KronoResult.Success).data.startsWith(" "))
    }

    private fun plan(): PriorityPlan {
        val start = LocalDateTime(2026, 10, 22, 14, 0)
        val report = PlannedTask(
            task = Task(
                id = "1",
                title = "Entrega informe trimestral",
                category = TaskCategory.ESTUDIO,
                priority = TaskPriority.ALTA,
                dueDate = LocalDateTime(2026, 10, 22, 18, 0),
            ),
            score = 9,
            factors = setOf(PriorityFactor.DUE_VERY_SOON, PriorityFactor.HIGH_PRIORITY),
            estimate = 45.minutes,
            estimateSource = EstimateSource.HISTORY,
            start = start,
            end = LocalDateTime(2026, 10, 22, 14, 45),
            timeUntilDue = 4.hours,
        )
        val meeting = report.copy(task = Task(id = "2", title = "Reunión de equipo"), factors = emptySet())
        return PriorityPlan(
            recommended = report,
            sequence = listOf(meeting),
            confidencePercent = 90,
            categoryBalance = mapOf(TaskCategory.ESTUDIO to 100, TaskCategory.TRABAJO to 0, TaskCategory.PERSONAL to 0),
        )
    }
}
