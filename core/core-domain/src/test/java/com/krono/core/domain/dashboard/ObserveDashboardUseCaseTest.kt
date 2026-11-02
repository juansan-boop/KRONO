package com.krono.core.domain.dashboard

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.FakeClock
import com.krono.core.domain.timer.FakeTimeSessionRepository
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [ObserveDashboardUseCase] (F-13 CA-1, CA-3 y CA-4). */
class ObserveDashboardUseCaseTest {

    private val tasks = FakeTaskRepository()
    private val sessions = FakeTimeSessionRepository()
    private val provider = CurrentDateTimeProvider(
        clock = FakeClock(Instant.parse("2026-10-30T12:00:00Z")),
        timeZone = { TimeZone.UTC },
    )
    private val observeDashboard = ObserveDashboardUseCase(tasks, sessions, provider)
    private val today = LocalDate(2026, 10, 30)

    private fun session(id: String, startedAt: String, minutes: Int) = TimeSession(
        id = id,
        taskId = "t",
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(startedAt),
        duration = minutes.minutes,
        type = TimeSessionType.ENFOQUE,
    )

    @Test
    fun combinaTareasDeHoyYTiempoDeEnfoqueDelDia() = runTest {
        tasks.givenTasks(
            Task(id = "a", title = "A", dueDate = LocalDateTime(2026, 10, 30, 10, 0), isCompleted = true),
            Task(id = "b", title = "B", dueDate = LocalDateTime(2026, 10, 30, 16, 0)),
        )
        sessions.givenSessions(
            session("hoy", "2026-10-30T09:00:00Z", 40),
            session("ayer", "2026-10-29T23:59:00Z", 90),
            session("manana", "2026-10-31T00:00:00Z", 90),
        )

        observeDashboard(today).test {
            val summary = (awaitItem() as KronoResult.Success).data
            assertEquals(1, summary.completedCount)
            assertEquals(1, summary.pendingCount)
            assertEquals(50, summary.completedPercent)
            assertEquals(40.minutes, summary.focusTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun errorAlLeerTareas_seEmiteComoError() = runTest {
        val failure = IllegalStateException("sin permiso")
        tasks.observeFailure = failure

        observeDashboard(today).test {
            val result = awaitItem()
            assertTrue(result is KronoResult.Error)
            assertEquals(failure, (result as KronoResult.Error).throwable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun errorAlLeerSesiones_seEmiteComoError() = runTest {
        sessions.observeFailure = IllegalStateException("sin permiso")

        observeDashboard(today).test {
            assertTrue(awaitItem() is KronoResult.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cuandoCambianLasTareas_seEmiteUnResumenNuevo() = runTest {
        observeDashboard(today).test {
            assertEquals(0, (awaitItem() as KronoResult.Success).data.pendingCount)
            tasks.givenTasks(Task(id = "a", title = "A", dueDate = LocalDateTime(2026, 10, 30, 10, 0)))
            assertEquals(1, (awaitItem() as KronoResult.Success).data.pendingCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
