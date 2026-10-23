package com.krono.core.domain.task

/**
 * Categorías funcionales admitidas para clasificar las tareas en KRONO (Sección 6 del SPEC).
 */
enum class TaskCategory {
    /** Tareas académicas, universitarias o de estudio. */
    ESTUDIO,
    /** Tareas vinculadas al trabajo remunerado o compromisos laborales. */
    TRABAJO,
    /** Tareas personales, del hogar o bienestar individual. */
    PERSONAL,
}
