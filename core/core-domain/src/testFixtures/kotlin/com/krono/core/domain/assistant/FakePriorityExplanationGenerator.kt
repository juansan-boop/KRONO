package com.krono.core.domain.assistant

import com.krono.core.common.KronoResult
import kotlinx.coroutines.CompletableDeferred

/**
 * Doble de prueba de [PriorityExplanationGenerator], compartido vía test fixtures.
 *
 * - [failure]: si no es nulo, [explain] devuelve `KronoResult.Error(failure)`.
 * - [gate]: si no es nulo, [explain] espera a que se complete.
 */
class FakePriorityExplanationGenerator(
    /** Texto que devuelve [explain] cuando no hay error forzado. */
    var explanation: String = "Explicación de prueba",
) : PriorityExplanationGenerator {

    /** Error forzado que devolverá [explain]; `null` para funcionamiento normal. */
    var failure: Throwable? = null

    /** Compuerta para pausar la respuesta en pruebas de concurrencia. */
    var gate: CompletableDeferred<Unit>? = null

    /** Planes recibidos, en orden de llegada. */
    val requests = mutableListOf<PriorityPlan>()

    /** Registra el plan y devuelve [explanation] o [failure]. */
    override suspend fun explain(plan: PriorityPlan): KronoResult<String> {
        requests += plan
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return KronoResult.Success(explanation)
    }
}
