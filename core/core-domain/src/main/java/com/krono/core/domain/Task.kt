package com.krono.core.domain

import kotlin.time.Duration

/**
 * Modelo de dominio de una tarea académica/laboral.
 * estimatedDuration: estimación (manual o generada por IA a partir de duraciones reales previas).
 * actualDuration: acumulado real medido por el cronómetro — es la base del aprendizaje de estimación.
 */
data class Task(
    val id: String,
    val title: String,
    val dueDate: kotlinx.datetime.LocalDateTime?,
    val estimatedDuration: Duration?,
    val actualDuration: Duration = Duration.ZERO,
    val isCompleted: Boolean = false,
)
