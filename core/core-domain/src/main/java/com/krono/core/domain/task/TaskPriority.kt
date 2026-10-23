package com.krono.core.domain.task

/**
 * Niveles de prioridad aplicables a una tarea en KRONO (Sección 6 del SPEC).
 */
enum class TaskPriority {
    /** Prioridad baja: tareas sin urgencia inmediata. */
    BAJA,
    /** Prioridad media: tareas estándar de la semana. */
    MEDIA,
    /** Prioridad alta: entregas críticas o próximas a vencer. */
    ALTA,
}
