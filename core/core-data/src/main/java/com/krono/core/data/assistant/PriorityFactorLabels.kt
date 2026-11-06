package com.krono.core.data.assistant

import com.krono.core.domain.assistant.PriorityFactor

/**
 * Motivo de la heurística en palabras para el modelo de lenguaje. Lo comparten la
 * explicación del asistente (F-12) y la herramienta `build_plan` del agente (F-28), para que
 * Gemini lea los mismos motivos en los dos casos.
 */
internal fun PriorityFactor.label(): String = when (this) {
    PriorityFactor.OVERDUE -> "la entrega ya venció"
    PriorityFactor.DUE_VERY_SOON -> "vence en pocas horas"
    PriorityFactor.DUE_TODAY -> "vence en menos de un día"
    PriorityFactor.DUE_SOON -> "vence en los próximos días"
    PriorityFactor.HIGH_PRIORITY -> "el usuario la marcó como prioridad alta"
    PriorityFactor.TIGHT_SCHEDULE -> "la estimación ocupa buena parte del margen que queda"
    PriorityFactor.CANNOT_FINISH_BY_DUE ->
        "ya no alcanza a terminarse antes de la entrega: la estimación es mayor que el tiempo que queda"
}
