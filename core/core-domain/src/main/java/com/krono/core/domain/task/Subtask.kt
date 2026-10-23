package com.krono.core.domain.task

/**
 * Subtarea que forma parte de una tarea principal y vive embebida en su documento (Sección 6 del SPEC).
 *
 * @property id Identificador único de la subtarea.
 * @property taskId Identificador de la tarea padre a la que pertenece.
 * @property title Título o descripción del paso a realizar.
 * @property isDone Indica si el paso ya fue completado.
 * @property position Posición ordinal para ordenar las subtareas dentro de la tarea.
 */
data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean = false,
    val position: Int = 0,
)
