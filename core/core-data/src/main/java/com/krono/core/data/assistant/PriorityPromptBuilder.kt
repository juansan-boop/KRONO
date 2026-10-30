package com.krono.core.data.assistant

import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.PriorityFactor
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import java.util.Locale
import javax.inject.Inject

/**
 * Construye la instrucción que recibe el modelo de lenguaje para explicar la tarea
 * recomendada (F-12).
 *
 * Solo envía los datos que la heurística ya usó (título, categoría, prioridad, entrega,
 * estimación y motivos) y le pide al modelo que no invente otros; por eso los diseños que
 * hablan de "ritmo circadiano" no se reproducen: la app no tiene esos datos (D-5).
 */
class PriorityPromptBuilder @Inject constructor() {

    /** Devuelve la instrucción completa para [plan]. */
    fun build(plan: PriorityPlan): String = buildString {
        appendLine(INSTRUCTIONS)
        appendLine()
        appendLine("Tarea recomendada:")
        appendLine(describe(plan.recommended))
        appendLine("Motivos detectados: ${plan.recommended.factors.joinToString { it.label() }.ifEmpty { "ninguno" }}")
        if (plan.sequence.isNotEmpty()) {
            appendLine()
            appendLine("Después siguen: ${plan.sequence.joinToString { "\"${it.task.title}\"" }}")
        }
    }

    private fun describe(planned: PlannedTask): String = buildString {
        append("- Título: \"${planned.task.title}\"\n")
        append("- Categoría: ${planned.task.category.label()}\n")
        append("- Prioridad asignada: ${planned.task.priority.label()}\n")
        append("- Entrega: ${planned.task.dueDate?.formatted() ?: "sin fecha"}\n")
        planned.timeUntilDue?.let { append("- Tiempo hasta la entrega: ${it.inWholeMinutes} minutos\n") }
        append("- Bloque sugerido: ${planned.start.formatted()} a ${planned.end.formatted()}")
        append(" (${planned.estimate.inWholeMinutes} min, ${planned.estimateSource.label()})")
    }

    private fun LocalDateTime.formatted(): String =
        String.format(Locale.ROOT, DATE_TIME_FORMAT, dayOfMonth, monthNumber, hour, minute)

    private fun TaskCategory.label(): String = when (this) {
        TaskCategory.ESTUDIO -> "Estudio"
        TaskCategory.TRABAJO -> "Trabajo"
        TaskCategory.PERSONAL -> "Personal"
    }

    private fun TaskPriority.label(): String = when (this) {
        TaskPriority.ALTA -> "alta"
        TaskPriority.MEDIA -> "media"
        TaskPriority.BAJA -> "baja"
    }

    private fun EstimateSource.label(): String = when (this) {
        EstimateSource.MANUAL -> "estimación del usuario"
        EstimateSource.HISTORY -> "promedio de su historial real en esa categoría"
        EstimateSource.DEFAULT -> "duración por defecto, aún sin historial"
    }

    private fun PriorityFactor.label(): String = when (this) {
        PriorityFactor.OVERDUE -> "la entrega ya venció"
        PriorityFactor.DUE_VERY_SOON -> "vence en pocas horas"
        PriorityFactor.DUE_TODAY -> "vence en menos de un día"
        PriorityFactor.DUE_SOON -> "vence en los próximos días"
        PriorityFactor.HIGH_PRIORITY -> "el usuario la marcó como prioridad alta"
        PriorityFactor.TIGHT_SCHEDULE -> "la estimación ocupa buena parte del margen que queda"
        PriorityFactor.CANNOT_FINISH_BY_DUE ->
            "ya no alcanza a terminarse antes de la entrega: la estimación es mayor que el tiempo que queda"
    }

    private companion object {
        const val DATE_TIME_FORMAT = "%02d/%02d %02d:%02d"

        val INSTRUCTIONS = """
            Eres el asistente de prioridades de KRONO, una app de gestión del tiempo para
            estudiantes universitarios que también trabajan.
            Explica en español, tuteando, en máximo dos frases y menos de
            ${FirebaseAiPriorityExplanationGenerator.MAX_EXPLANATION_LENGTH} caracteres, por qué
            conviene empezar ahora la tarea recomendada.
            Usa solo los datos de abajo. No inventes datos biológicos, de energía ni de ritmo
            circadiano, ni porcentajes. Sin saludos, sin emojis y sin formato Markdown.
        """.trimIndent()
    }
}
