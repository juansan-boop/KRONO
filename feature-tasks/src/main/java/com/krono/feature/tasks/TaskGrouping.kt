package com.krono.feature.tasks

import com.krono.core.domain.task.Task
import kotlinx.datetime.LocalDate

/**
 * Utilidades puras para clasificar y agrupar tareas por temporalidad (F-03 CA-1).
 */
object TaskTimeClassifier {

    /**
     * Determina si una tarea corresponde a la sección "Para Hoy".
     * Incluye tareas cuya fecha límite es hoy o anterior (pendientes que vencen hoy o están vencidas).
     */
    fun isToday(task: Task, today: LocalDate): Boolean {
        val dueDate = task.dueDate ?: return false
        return dueDate.date <= today
    }

    /**
     * Determina si una tarea corresponde a la sección "Esta Semana".
     * Incluye tareas para días posteriores a hoy o sin fecha límite programada.
     */
    fun isThisWeek(task: Task, today: LocalDate): Boolean {
        val dueDate = task.dueDate ?: return true
        return dueDate.date > today
    }
}

/**
 * Calculador del porcentaje de Enfoque Diario (F-03 CA-3).
 */
object DailyFocusCalculator {

    /**
     * Calcula el porcentaje de completitud de las tareas de hoy (0 a 100).
     *
     * @param todayTasks Lista de tareas clasificadas para hoy.
     * @return Porcentaje entero redondeado. Si no hay tareas de hoy, devuelve 0.
     */
    fun calculatePercentage(todayTasks: List<Task>): Int {
        if (todayTasks.isEmpty()) return 0
        val completedCount = todayTasks.count { it.isCompleted }
        return ((completedCount.toDouble() / todayTasks.size) * 100).toInt()
    }

    /**
     * Retorna el conteo de tareas de hoy que aún están pendientes de completar.
     *
     * @param todayTasks Lista de tareas de hoy.
     * @return Número de tareas no completadas.
     */
    fun countPending(todayTasks: List<Task>): Int =
        todayTasks.count { !it.isCompleted }
}
