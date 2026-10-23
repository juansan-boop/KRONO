package com.krono.core.data.task

import com.krono.core.domain.task.Subtask

/**
 * DTO para la serialización de subtareas embebidas dentro de un documento de tarea en Cloud Firestore.
 *
 * @property id Identificador único de la subtarea.
 * @property taskId Identificador de la tarea padre.
 * @property title Título o descripción del paso.
 * @property isDone Estado de completitud.
 * @property position Orden de visualización.
 */
data class SubtaskDto(
    val id: String = "",
    val taskId: String = "",
    val title: String = "",
    val isDone: Boolean = false,
    val position: Int = 0,
) {
    /** Convierte el DTO al modelo de dominio [Subtask]. */
    fun toDomain(): Subtask = Subtask(
        id = id,
        taskId = taskId,
        title = title,
        isDone = isDone,
        position = position,
    )

    /** Serializa el DTO a un mapa compatible con Firestore. */
    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_TASK_ID to taskId,
        FIELD_TITLE to title,
        FIELD_IS_DONE to isDone,
        FIELD_POSITION to position,
    )

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_TASK_ID = "taskId"
        const val FIELD_TITLE = "title"
        const val FIELD_IS_DONE = "isDone"
        const val FIELD_POSITION = "position"

        /** Crea una instancia a partir del modelo de dominio [Subtask]. */
        fun fromDomain(subtask: Subtask): SubtaskDto = SubtaskDto(
            id = subtask.id,
            taskId = subtask.taskId,
            title = subtask.title,
            isDone = subtask.isDone,
            position = subtask.position,
        )

        /** Reconstruye un [SubtaskDto] a partir de un mapa de Firestore. */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): SubtaskDto = SubtaskDto(
            id = map[FIELD_ID] as? String ?: "",
            taskId = map[FIELD_TASK_ID] as? String ?: "",
            title = map[FIELD_TITLE] as? String ?: "",
            isDone = map[FIELD_IS_DONE] as? Boolean ?: false,
            position = (map[FIELD_POSITION] as? Number)?.toInt() ?: 0,
        )
    }
}
