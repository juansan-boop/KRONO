package com.krono.feature.tasks

import com.krono.core.domain.task.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas de [TaskTimeClassifier]: cada tarea cae en un único grupo de la lista (F-03 CA-1). */
class TaskTimeClassifierTest {

    private val today = LocalDate(2026, 10, 30)

    private fun task(day: Int, isCompleted: Boolean = false, hasDueDate: Boolean = true) = Task(
        id = "t$day",
        title = "Tarea $day",
        isCompleted = isCompleted,
        dueDate = if (hasDueDate) LocalDateTime(2026, 10, day, 12, 0) else null,
    )

    private fun groupOf(task: Task): String = listOf(
        "hoy" to TaskTimeClassifier.isToday(task, today),
        "completadas" to TaskTimeClassifier.isCompletedEarlier(task, today),
        "semana" to TaskTimeClassifier.isThisWeek(task, today),
    ).filter { it.second }.map { it.first }.single()

    @Test
    fun unaVencidaPendiente_vaEnParaHoy() = assertEquals("hoy", groupOf(task(day = 28)))

    @Test
    fun unaDeHoy_vaEnParaHoyEstePendienteOHecha() {
        assertEquals("hoy", groupOf(task(day = 30)))
        assertEquals("hoy", groupOf(task(day = 30, isCompleted = true)))
    }

    @Test
    fun unaCompletadaDeUnDiaAnterior_vaEnCompletadasYNoEnParaHoy() =
        assertEquals("completadas", groupOf(task(day = 28, isCompleted = true)))

    @Test
    fun unaFuturaOSinFecha_vaEnEstaSemana() {
        assertEquals("semana", groupOf(task(day = 31)))
        assertEquals("semana", groupOf(task(day = 1, hasDueDate = false)))
    }
}
