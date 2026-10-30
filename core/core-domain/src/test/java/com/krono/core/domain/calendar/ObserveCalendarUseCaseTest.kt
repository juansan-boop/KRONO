package com.krono.core.domain.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskPriority
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [ObserveCalendarUseCase] (F-16 CA-1 y CA-2). */
class ObserveCalendarUseCaseTest {

    private val repository = FakeTaskRepository()
    private val observeCalendar = ObserveCalendarUseCase(repository)
    private val day = CalendarPeriod(CalendarViewMode.DAY, LocalDate(2026, 10, 30))

    @Test
    fun tareaDentroDelPeriodo_apareceComoBloque() = runTest {
        repository.givenTasks(task("a", at(10, 0), estimate = 1.hours))

        observeCalendar(day).test {
            val schedule = nextSchedule()
            assertEquals(listOf("a"), schedule.blocks.map { it.taskId })
            assertEquals(at(9, 0), schedule.blocks.single().start)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tareaSinFecha_noApareceEnElCalendario() = runTest {
        repository.givenTasks(Task(id = "sin", title = "Sin fecha"))

        observeCalendar(day).test {
            assertTrue(nextSchedule().blocks.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tareasFueraDelPeriodo_seExcluyen() = runTest {
        repository.givenTasks(
            task("ayer", LocalDateTime(2026, 10, 29, 10, 0)),
            task("manana", LocalDateTime(2026, 10, 31, 10, 0)),
        )

        observeCalendar(day).test {
            assertTrue(nextSchedule().blocks.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bordesDelPeriodo_inicioInclusivoYFinExclusivo() = runTest {
        repository.givenTasks(
            // 23:30-00:00 del día anterior: termina justo cuando empieza el período.
            task("termina", LocalDateTime(2026, 10, 30, 0, 0)),
            // 00:00-01:00 del día del período.
            task("empieza", LocalDateTime(2026, 10, 30, 1, 0), estimate = 1.hours),
            // 23:30-00:00 del propio día: termina en el borde final y pertenece al día.
            task("cierra", LocalDateTime(2026, 10, 31, 0, 0)),
            // 00:00-00:30 del día siguiente: empieza justo en el fin exclusivo.
            task("siguiente", LocalDateTime(2026, 10, 31, 0, 30)),
        )

        observeCalendar(day).test {
            assertEquals(listOf("empieza", "cierra"), nextSchedule().blocks.map { it.taskId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bloqueQueCruzaLaMedianoche_apareceEnLaAgendaDeAmbosDias() = runTest {
        repository.givenTasks(task("noche", LocalDateTime(2026, 10, 31, 1, 0), estimate = 3.hours))
        val week = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2026, 10, 30))

        observeCalendar(week).test {
            val schedule = nextSchedule()
            assertEquals(1, schedule.blocks.size)
            assertEquals(listOf("noche"), schedule.blocksOn(LocalDate(2026, 10, 30)).map { it.taskId })
            assertEquals(listOf("noche"), schedule.blocksOn(LocalDate(2026, 10, 31)).map { it.taskId })
            assertTrue(schedule.blocksOn(LocalDate(2026, 11, 1)).isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ordenaPorInicioYLuegoPorPrioridadAltaPrimero() = runTest {
        repository.givenTasks(
            task("tarde", at(16, 0), estimate = 1.hours),
            task("baja", at(10, 0), estimate = 1.hours, priority = TaskPriority.BAJA),
            task("alta", at(10, 0), estimate = 1.hours, priority = TaskPriority.ALTA),
            task("media", at(10, 0), estimate = 1.hours, priority = TaskPriority.MEDIA),
        )

        observeCalendar(day).test {
            assertEquals(listOf("alta", "media", "baja", "tarde"), nextSchedule().blocks.map { it.taskId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tareasCompletadas_seIncluyenConSuBandera() = runTest {
        repository.givenTasks(task("hecha", at(10, 0)).copy(isCompleted = true))

        observeCalendar(day).test {
            assertTrue(nextSchedule().blocks.single().isCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun vistaMensual_agrupaPorDiaIncluyendoDiasVaciosYVecinos() = runTest {
        repository.givenTasks(
            task("fin-sep", LocalDateTime(2024, 9, 30, 10, 0)),
            task("jueves", LocalDateTime(2024, 10, 24, 10, 0)),
        )
        val month = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2024, 10, 24))

        observeCalendar(month).test {
            val schedule = nextSchedule()
            assertEquals(month.days.toSet(), schedule.blocksByDay.keys)
            assertEquals(listOf("fin-sep"), schedule.blocksOn(LocalDate(2024, 9, 30)).map { it.taskId })
            assertTrue(schedule.blocksOn(LocalDate(2024, 10, 1)).isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun errorDelRepositorio_sePropagaComoErrorYNoComoListaVacia() = runTest {
        val failure = IllegalStateException("sin permisos")
        repository.observeFailure = failure

        observeCalendar(day).test {
            assertEquals(KronoResult.Error(failure), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun alCambiarLasTareas_emiteUnaNuevaAgenda() = runTest {
        repository.givenTasks(task("a", at(10, 0)))

        observeCalendar(day).test {
            assertEquals(1, nextSchedule().blocks.size)
            repository.givenTasks(task("b", at(12, 0)))
            assertEquals(2, nextSchedule().blocks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lasFechasSeTratanComoLocalesSinConversionDeZona() = runTest {
        // Sin conversión de zona: en el día de cambio de hora, un bloque de las 02:30 sigue
        // empezando y terminando en la hora de pared indicada, sin saltos.
        repository.givenTasks(task("dst", LocalDateTime(2026, 3, 8, 2, 30), estimate = 30.minutes))
        val period = CalendarPeriod(CalendarViewMode.DAY, LocalDate(2026, 3, 8))

        observeCalendar(period).test {
            val block = nextSchedule().blocks.single()
            assertEquals(LocalDateTime(2026, 3, 8, 2, 0), block.start)
            assertEquals(LocalDateTime(2026, 3, 8, 2, 30), block.end)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun at(hour: Int, minute: Int) = LocalDateTime(2026, 10, 30, hour, minute)

    private fun task(
        id: String,
        due: LocalDateTime,
        estimate: Duration? = null,
        priority: TaskPriority = TaskPriority.MEDIA,
    ) = Task(id = id, title = id, dueDate = due, estimatedDuration = estimate, priority = priority)

    private suspend fun ReceiveTurbine<KronoResult<CalendarSchedule>>.nextSchedule(): CalendarSchedule =
        (awaitItem() as KronoResult.Success).data
}
