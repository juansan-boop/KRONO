package com.krono.feature.tasks.assistant

import com.krono.core.domain.assistant.PriorityPlan

/**
 * Estado de la pantalla del asistente de prioridades (F-12).
 *
 * @property isLoading se están leyendo las tareas por primera vez.
 * @property hasError no se pudieron leer las tareas.
 * @property plan plan calculado, o `null` si no hay tareas pendientes.
 * @property explanation estado de la explicación de la tarea recomendada.
 */
data class PriorityAssistantUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val plan: PriorityPlan? = null,
    val explanation: ExplanationState = ExplanationState.Loading,
) {
    /** No hay tareas pendientes que planificar. */
    val isEmpty: Boolean get() = !isLoading && !hasError && plan == null
}

/** Estado de la explicación que redacta la IA (D-5 opción híbrida). */
sealed interface ExplanationState {
    /** Esperando la respuesta del modelo. */
    data object Loading : ExplanationState

    /** El modelo redactó [text]. */
    data class Generated(val text: String) : ExplanationState

    /** El modelo no respondió: la UI arma la explicación con plantillas a partir de los motivos. */
    data object Fallback : ExplanationState
}
