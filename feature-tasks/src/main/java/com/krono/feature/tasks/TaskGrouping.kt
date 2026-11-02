package com.krono.feature.tasks

import com.krono.core.domain.task.Task
import kotlinx.datetime.LocalDate

/**
 * Utilidades puras para agrupar tareas por temporalidad (F-03 CA-1).
 *
 * Cada tarea cae en exactamente un grupo:
 * - "Para Hoy": vence hoy (hecha o no) o está vencida y sigue pendiente, porque pide atención hoy.
 * - "Completadas": completada con fecha límite de un día anterior; es historial y no debe inflar
 *   "Para Hoy" ni el avance del día.
 * - "Esta Semana": vence después de hoy o no tiene fecha límite.
 */
object TaskTimeClassifier {

    /**
     * Indica si la tarea va en "Para Hoy": vence hoy, o venció antes y no está completada.
     */
    fun isToday(task: Task, today: LocalDate): Boolean {
        val dueDate = task.dueDate?.date ?: return false
        return dueDate == today || (dueDate < today && !task.isCompleted)
    }

    /**
     * Indica si la tarea va en "Completadas": completada y con fecha límite anterior a hoy.
     */
    fun isCompletedEarlier(task: Task, today: LocalDate): Boolean {
        val dueDate = task.dueDate?.date ?: return false
        return dueDate < today && task.isCompleted
    }

    /**
     * Indica si la tarea va en "Esta Semana": vence después de hoy o no tiene fecha límite.
     */
    fun isThisWeek(task: Task, today: LocalDate): Boolean {
        val dueDate = task.dueDate?.date ?: return true
        return dueDate > today
    }
}
