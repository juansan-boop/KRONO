package com.krono.core.domain.dashboard

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [DashboardSummary] (F-13 CA-1 y CA-3). */
class DashboardSummaryTest {

    private val today = LocalDate(2026, 10, 30)

    private fun task(
        id: String,
        due: LocalDateTime?,
        priority: TaskPriority = TaskPriority.MEDIA,
        completed: Boolean = false,
    ) = Task(id = id, title = id, dueDate = due, priority = priority, isCompleted = completed)

    private fun at(hour: Int, minute: Int = 0, day: Int = 30) = LocalDateTime(2026, 10, day, hour, minute)

    private fun session(minutes: Int) = TimeSession(
        id = "s$minutes",
        taskId = "t",
        startedAt = Instant.parse("2026-10-30T10:00:00Z"),
        endedAt = Instant.parse("2026-10-30T11:00:00Z"),
        duration = minutes.minutes,
        type = TimeSessionType.CRONOMETRO,
    )

    @Test
    fun sinTareasHoy_todoEnCeroYSinPorcentaje() {
        val summary = DashboardSummary.from(emptyList(), emptyList(), today)

        assertEquals(0, summary.completedCount)
        assertEquals(0, summary.pendingCount)
        assertEquals(0, summary.completedPercent)
        assertEquals(Duration.ZERO, summary.focusTime)
        assertEquals(emptyList<Task>(), summary.priorityTasks)
    }

    @Test
    fun soloCuentaLasTareasConFechaLimiteHoy() {
        val tasks = listOf(
            task("hoy", at(10)),
            task("ayer", at(10, day = 29)),
            task("manana", at(10, day = 31)),
            task("sinFecha", null),
        )

        val summary = DashboardSummary.from(tasks, emptyList(), today)

        assertEquals(1, summary.pendingCount)
        assertEquals(0, summary.completedCount)
    }

    @Test
    fun porcentajeEsCompletadasSobreTotalDeHoyRedondeado() {
        val tasks = listOf(
            task("a", at(9), completed = true),
            task("b", at(10), completed = true),
            task("c", at(11)),
        )

        val summary = DashboardSummary.from(tasks, emptyList(), today)

        assertEquals(2, summary.completedCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(67, summary.completedPercent)
    }

    @Test
    fun todasCompletadas_cienPorCiento() {
        val summary = DashboardSummary.from(listOf(task("a", at(9), completed = true)), emptyList(), today)

        assertEquals(100, summary.completedPercent)
    }

    @Test
    fun tiempoDeEnfoqueSumaLasSesiones() {
        val summary = DashboardSummary.from(emptyList(), listOf(session(25), session(30)), today)

        assertEquals(55.minutes, summary.focusTime)
    }

    @Test
    fun prioritarias_sonLasPendientesDeHoyPorPrioridadYLuegoHora() {
        val tasks = listOf(
            task("mediaTemprano", at(8), TaskPriority.MEDIA),
            task("altaTarde", at(18), TaskPriority.ALTA),
            task("altaTemprano", at(9), TaskPriority.ALTA),
            task("bajaCompletada", at(7), TaskPriority.BAJA, completed = true),
            task("otroDia", at(9, day = 31), TaskPriority.ALTA),
        )

        val summary = DashboardSummary.from(tasks, emptyList(), today)

        assertEquals(listOf("altaTemprano", "altaTarde", "mediaTemprano"), summary.priorityTasks.map { it.id })
    }
}
