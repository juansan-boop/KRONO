package com.krono.core.domain

import kotlin.time.Duration

/**
 * Modelo de dominio de una tarea académica/laboral.
 * estimatedDuration: estimación (manual o generada por IA a partir de duraciones reales previas).
 * actualDuration: acumulado real medido por el cronómetro — es la base del aprendizaje de estimación.
 */
data class Task(
    /** Identificador único de la tarea. */
    val id: String,
    /** Título visible de la tarea. */
    val title: String,
    /** Fecha límite de entrega, o `null` si no tiene. */
    val dueDate: kotlinx.datetime.LocalDateTime?,
    /** Duración estimada, o `null` si aún no hay estimación. */
    val estimatedDuration: Duration?,
    /** Tiempo real acumulado por el cronómetro. */
    val actualDuration: Duration = Duration.ZERO,
    /** Indica si la tarea ya se terminó. */
    val isCompleted: Boolean = false,
)
