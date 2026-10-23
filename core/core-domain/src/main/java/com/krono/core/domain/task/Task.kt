package com.krono.core.domain.task

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/**
 * Modelo de dominio de una tarea en KRONO (Sección 6 del SPEC).
 *
 * @property id Identificador único de la tarea.
 * @property title Título visible de la tarea.
 * @property category Categoría funcional de la tarea (Estudio, Trabajo o Personal).
 * @property priority Nivel de prioridad asignado (Baja, Media o Alta).
 * @property dueDate Fecha y hora límite de entrega, o `null` si no tiene.
 * @property estimatedDuration Estimación de duración (manual o por IA), o `null` si aún no hay estimación.
 * @property actualDuration Tiempo real acumulado medido por el cronómetro.
 * @property isCompleted Indica si la tarea ya se terminó.
 * @property note Nota rápida o descripción adicional de la tarea, o `null` si no tiene.
 * @property subtasks Lista de subtareas embebidas asociadas a esta tarea.
 */
data class Task(
    val id: String,
    val title: String,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val priority: TaskPriority = TaskPriority.MEDIA,
    val dueDate: LocalDateTime? = null,
    val estimatedDuration: Duration? = null,
    val actualDuration: Duration = Duration.ZERO,
    val isCompleted: Boolean = false,
    val note: String? = null,
    val subtasks: List<Subtask> = emptyList(),
)
