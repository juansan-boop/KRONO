package com.krono.core.domain.assistant.agent

import com.krono.core.domain.assistant.PriorityPlan

/**
 * Resultado de una petición al agente de planificación (F-28): las tareas que propone crear,
 * el plan H2 que quedaría al crearlas y el mensaje que redactó el modelo.
 *
 * @property drafts tareas propuestas, todas ya estimadas por H1.
 * @property plan plan de prioridades con las tareas actuales más las propuestas, o `null` si
 * no habría tareas pendientes.
 * @property message respuesta del modelo para el usuario, o `null` si no redactó ninguna.
 */
data class AgentProposal(
    val drafts: List<TaskDraft>,
    val plan: PriorityPlan?,
    val message: String?,
) {
    /** Identificadores de las tareas del plan que son nuevas, para marcarlas en la UI. */
    val newTaskIds: Set<String> get() = drafts.mapTo(mutableSetOf()) { it.id }
}
