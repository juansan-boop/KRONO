package com.krono.core.domain.calendar

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas de la derivación de bloques desde tareas y de las consultas de [CalendarBlock] (F-16). */
class CalendarBlockTest {

    private val due = LocalDateTime(2026, 10, 30, 15, 0)

    @Test
    fun conEstimacion_elBloqueTerminaEnLaFechaLimiteYEmpiezaAntes() {
        val block = Task(id = "t", title = "Informe", dueDate = due, estimatedDuration = 90.minutes)
            .toCalendarBlock()!!

        assertEquals(LocalDateTime(2026, 10, 30, 13, 30), block.start)
        assertEquals(due, block.end)
        assertEquals(BlockDurationSource.ESTIMATED, block.durationSource)
        assertEquals(90.minutes, block.duration)
    }

    @Test
    fun sinEstimacion_ocupa30MinutosPorDefecto() {
        val block = Task(id = "t", title = "Informe", dueDate = due).toCalendarBlock()!!

        assertEquals(LocalDateTime(2026, 10, 30, 14, 30), block.start)
        assertEquals(due, block.end)
        assertEquals(BlockDurationSource.DEFAULT, block.durationSource)
        assertEquals(30.minutes, DEFAULT_BLOCK_DURATION)
    }

    @Test
    fun estimacionCeroONegativa_seTrataComoSinEstimacion() {
        val zero = Task(id = "t", title = "x", dueDate = due, estimatedDuration = 0.minutes).toCalendarBlock()!!
        val negative = Task(id = "t", title = "x", dueDate = due, estimatedDuration = (-5).minutes).toCalendarBlock()!!

        assertEquals(BlockDurationSource.DEFAULT, zero.durationSource)
        assertEquals(BlockDurationSource.DEFAULT, negative.durationSource)
        assertEquals(DEFAULT_BLOCK_DURATION, negative.duration)
    }

    @Test
    fun sinFechaLimite_noGeneraBloque() {
        assertNull(Task(id = "t", title = "x", estimatedDuration = 1.hours).toCalendarBlock())
    }

    @Test
    fun elBloqueConservaLosDatosDeLaTareaDeOrigen() {
        val task = Task(id = "t", title = "x", dueDate = due, priority = TaskPriority.ALTA, isCompleted = true)

        val block = task.toCalendarBlock()!!

        assertEquals(task, block.task)
        assertEquals("t", block.taskId)
        assertEquals(TaskPriority.ALTA, block.priority)
        assertTrue(block.isCompleted)
    }

    @Test
    fun bloqueQueCruzaLaMedianoche_perteneceAAmbosDias() {
        val block = Task(id = "t", title = "x", dueDate = LocalDateTime(2026, 10, 31, 1, 0), estimatedDuration = 3.hours)
            .toCalendarBlock()!!

        assertEquals(LocalDateTime(2026, 10, 30, 22, 0), block.start)
        assertTrue(block.occursOn(LocalDate(2026, 10, 30)))
        assertTrue(block.occursOn(LocalDate(2026, 10, 31)))
        assertFalse(block.occursOn(LocalDate(2026, 10, 29)))
        assertFalse(block.occursOn(LocalDate(2026, 11, 1)))
    }

    @Test
    fun bloqueQueTerminaExactoALaMedianoche_noPerteneceAlDiaSiguiente() {
        val block = Task(id = "t", title = "x", dueDate = LocalDateTime(2026, 10, 31, 0, 0), estimatedDuration = 1.hours)
            .toCalendarBlock()!!

        assertTrue(block.occursOn(LocalDate(2026, 10, 30)))
        assertFalse(block.occursOn(LocalDate(2026, 10, 31)))
    }

    @Test
    fun bloqueQueEmpiezaExactoALaMedianoche_perteneceAEseDia() {
        val block = Task(id = "t", title = "x", dueDate = LocalDateTime(2026, 10, 31, 1, 0), estimatedDuration = 1.hours)
            .toCalendarBlock()!!

        assertEquals(LocalDateTime(2026, 10, 31, 0, 0), block.start)
        assertTrue(block.occursOn(LocalDate(2026, 10, 31)))
        assertFalse(block.occursOn(LocalDate(2026, 10, 30)))
    }

    @Test
    fun enCurso_antesDelInicio_noEstaEnCurso() {
        val block = blockOf(start = LocalDateTime(2026, 10, 30, 14, 0), end = LocalDateTime(2026, 10, 30, 15, 0))

        assertFalse(block.isInProgress(LocalDateTime(2026, 10, 30, 13, 59)))
    }

    @Test
    fun enCurso_enElInstanteExactoDeInicio_estaEnCurso() {
        val block = blockOf(start = LocalDateTime(2026, 10, 30, 14, 0), end = LocalDateTime(2026, 10, 30, 15, 0))

        assertTrue(block.isInProgress(LocalDateTime(2026, 10, 30, 14, 0)))
    }

    @Test
    fun enCurso_dentroDelIntervalo_estaEnCurso() {
        val block = blockOf(start = LocalDateTime(2026, 10, 30, 14, 0), end = LocalDateTime(2026, 10, 30, 15, 0))

        assertTrue(block.isInProgress(LocalDateTime(2026, 10, 30, 14, 30)))
    }

    @Test
    fun enCurso_enElBordeExactoDeFin_yaNoEstaEnCurso() {
        val block = blockOf(start = LocalDateTime(2026, 10, 30, 14, 0), end = LocalDateTime(2026, 10, 30, 15, 0))

        assertFalse(block.isInProgress(LocalDateTime(2026, 10, 30, 15, 0)))
    }

    @Test
    fun enCurso_despuesDelFin_noEstaEnCurso() {
        val block = blockOf(start = LocalDateTime(2026, 10, 30, 14, 0), end = LocalDateTime(2026, 10, 30, 15, 0))

        assertFalse(block.isInProgress(LocalDateTime(2026, 10, 30, 15, 1)))
    }

    @Test
    fun enCurso_soloMiraElIntervalo_unaTareaCompletadaDentroDelIntervaloDevuelveTrue() {
        val start = LocalDateTime(2026, 10, 30, 14, 0)
        val end = LocalDateTime(2026, 10, 30, 15, 0)
        val completed = CalendarBlock(
            Task(id = "t", title = "x", dueDate = end, isCompleted = true),
            start,
            end,
            BlockDurationSource.ESTIMATED,
        )

        assertTrue(completed.isInProgress(LocalDateTime(2026, 10, 30, 14, 30)))
    }

    private fun blockOf(start: LocalDateTime, end: LocalDateTime) =
        CalendarBlock(Task(id = "t", title = "x", dueDate = end), start, end, BlockDurationSource.ESTIMATED)
}
