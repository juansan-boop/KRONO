package com.krono.core.domain.task

/**
 * Campos de una [Task] que se pueden escribir de forma parcial, sin reescribir el documento
 * completo (F-05 CA-2).
 *
 * Una escritura parcial solo toca los campos indicados: así, por ejemplo, editar el título no
 * pisa el `actualDuration` que el cronómetro (F-08) suma en el servidor mientras tanto.
 * `actualDuration` no está aquí a propósito: solo lo modifica el cronómetro.
 */
enum class TaskField {
    /** Título visible de la tarea. */
    TITLE,

    /** Categoría (Estudio, Trabajo o Personal). */
    CATEGORY,

    /** Nivel de prioridad. */
    PRIORITY,

    /** Fecha y hora límite de entrega. */
    DUE_DATE,

    /** Duración estimada. */
    ESTIMATED_DURATION,

    /** Estado de completitud. */
    IS_COMPLETED,

    /** Nota rápida. */
    NOTE,

    /** Lista de subtareas embebidas, que se escribe completa. */
    SUBTASKS,
    ;

    companion object {
        /** Campos que modifica el formulario de edición de tareas (F-05). */
        val EDIT_FORM: Set<TaskField> = setOf(TITLE, CATEGORY, PRIORITY, DUE_DATE, ESTIMATED_DURATION)
    }
}
