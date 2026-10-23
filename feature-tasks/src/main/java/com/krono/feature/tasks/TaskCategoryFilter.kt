package com.krono.feature.tasks

import com.krono.core.domain.task.TaskCategory

/**
 * Opciones de filtrado por categoría en la lista de tareas (F-03 CA-2).
 */
enum class TaskCategoryFilter {
    /** Muestra todas las tareas sin filtrar por categoría. */
    TODAS,
    /** Filtra únicamente tareas de estudio o académicas. */
    ESTUDIO,
    /** Filtra únicamente tareas laborales. */
    TRABAJO,
    /** Filtra únicamente tareas personales. */
    PERSONAL;

    /** Mapea el filtro a su categoría de dominio correspondiente, o `null` para [TODAS]. */
    fun toDomainCategory(): TaskCategory? = when (this) {
        TODAS -> null
        ESTUDIO -> TaskCategory.ESTUDIO
        TRABAJO -> TaskCategory.TRABAJO
        PERSONAL -> TaskCategory.PERSONAL
    }
}
