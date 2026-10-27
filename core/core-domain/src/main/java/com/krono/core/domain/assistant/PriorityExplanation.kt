package com.krono.core.domain.assistant

import com.krono.core.common.KronoResult

/**
 * Redacta en lenguaje natural por qué conviene hacer ahora la tarea recomendada (F-12,
 * D-5 opción híbrida). La implementación usa un modelo de lenguaje; el orden del plan lo
 * decide siempre la heurística local, nunca el modelo.
 */
interface PriorityExplanationGenerator {

    /**
     * Genera la explicación de [plan].
     *
     * @return el texto en español, o [KronoResult.Error] con un [AssistantError] si el
     * servicio no responde o la respuesta no sirve; la UI usa entonces su texto de plantilla.
     */
    suspend fun explain(plan: PriorityPlan): KronoResult<String>
}

/** Errores del servicio que redacta las explicaciones del asistente. */
sealed class AssistantError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** El servicio de IA no respondió (sin conexión, tiempo agotado o servicio deshabilitado). */
    class Unavailable(cause: Throwable? = null) :
        AssistantError("El asistente de IA no está disponible en este momento.", cause)

    /** La respuesta llegó vacía o fuera de los límites esperados. */
    data object InvalidResponse : AssistantError("El asistente de IA devolvió una respuesta no válida.")
}

/** Caso de uso que pide la explicación de la tarea recomendada (F-12). */
class ExplainPriorityUseCase(
    private val generator: PriorityExplanationGenerator,
) {
    /** Devuelve la explicación de [plan] o el error que impidió generarla. */
    suspend operator fun invoke(plan: PriorityPlan): KronoResult<String> = generator.explain(plan)
}
