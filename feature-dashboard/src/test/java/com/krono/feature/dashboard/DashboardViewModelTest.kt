package com.krono.feature.dashboard

import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.dashboard.ObserveDashboardUseCase
import com.krono.core.domain.profile.FakeUserNameRepository
import com.krono.core.domain.profile.ObserveUserNameUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.timer.FakeClock
import com.krono.core.domain.timer.FakeTimeSessionRepository
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [DashboardViewModel]: resumen del día, prioritarias y estados (F-13 CA-1, CA-3 y CA-4). */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tasks = FakeTaskRepository()
    private val sessions = FakeTimeSessionRepository()
    private val userName = FakeUserNameRepository()
    private val clock = FakeClock(Instant.parse("2026-10-30T12:00:00Z"))

    private fun TestScope.started(): DashboardViewModel {
        val vm = DashboardViewModel(
            observeDashboard = ObserveDashboardUseCase(tasks, sessions, provider()),
            observeUserName = ObserveUserNameUseCase(userName),
            dateTimeProvider = provider(),
        )
        // WhileSubscribed: hay que mantener a alguien suscrito para que el estado avance.
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    private fun provider() = CurrentDateTimeProvider(clock) { TimeZone.UTC }

    private fun task(
        id: String,
        due: LocalDateTime?,
        priority: TaskPriority = TaskPriority.MEDIA,
        completed: Boolean = false,
    ) = Task(
        id = id,
        title = "Tarea $id",
        category = TaskCategory.TRABAJO,
        priority = priority,
        dueDate = due,
        isCompleted = completed,
    )

    private fun today(hour: Int) = LocalDateTime(2026, 10, 30, hour, 30)

    private val DashboardViewModel.content get() = uiState.value as DashboardUiState.Content

    @Test
    fun alAbrir_antesDeQueLleguenLosDatos_estaCargando() = runTest {
        val vm = DashboardViewModel(
            ObserveDashboardUseCase(tasks, sessions, provider()),
            ObserveUserNameUseCase(userName),
            provider(),
        )

        assertEquals(DashboardUiState.Loading, vm.uiState.value)
    }

    @Test
    fun alCargar_muestraSaludoPorcentajeConteosYTiempoDeEnfoque() = runTest {
        userName.name.value = "Alexa"
        tasks.givenTasks(
            task("a", today(9), completed = true),
            task("b", today(10), completed = true),
            task("c", today(11)),
            task("d", today(12)),
        )
        sessions.givenSessions(
            TimeSession(
                id = "s",
                taskId = "a",
                startedAt = Instant.parse("2026-10-30T08:00:00Z"),
                endedAt = Instant.parse("2026-10-30T09:30:00Z"),
                duration = 90.minutes,
                type = TimeSessionType.ENFOQUE,
            ),
        )

        val content = started().content

        assertEquals("Alexa", content.userName)
        assertEquals(50, content.completedPercent)
        assertEquals(2, content.completedCount)
        assertEquals(2, content.pendingCount)
        assertEquals(90.minutes, content.focusTime)
        assertTrue(content.hasTasksToday)
    }

    @Test
    fun prioritarias_estanOrdenadasYLimitadasATres() = runTest {
        tasks.givenTasks(
            task("mediaTemprano", today(8), TaskPriority.MEDIA),
            task("altaTarde", today(18), TaskPriority.ALTA),
            task("altaTemprano", today(9), TaskPriority.ALTA),
            task("baja", today(7), TaskPriority.BAJA),
        )

        val items = started().content.priorityTasks

        assertEquals(listOf("altaTemprano", "altaTarde", "mediaTemprano"), items.map { it.taskId })
        assertEquals(LocalTime(9, 30), items.first().dueTime)
        assertEquals(TaskCategory.TRABAJO, items.first().category)
        assertEquals(TaskPriority.ALTA, items.first().priority)
    }

    @Test
    fun sinTareasHoy_elEstadoEsVacioPeroConSaludo() = runTest {
        userName.name.value = "Alexa"

        val content = started().content

        assertEquals(false, content.hasTasksToday)
        assertEquals(emptyList<PriorityTaskUi>(), content.priorityTasks)
        assertEquals("Alexa", content.userName)
    }

    @Test
    fun sinNombre_elSaludoNoTieneNombre() = runTest {
        assertNull(started().content.userName)
    }

    @Test
    fun alCambiarElNombreDelPerfil_seActualizaElSaludo() = runTest {
        val vm = started()

        userName.name.value = "Alex"
        runCurrent()

        assertEquals("Alex", vm.content.userName)
    }

    @Test
    fun cuandoUnaTareaSeCompleta_elResumenSeActualiza() = runTest {
        tasks.givenTasks(task("a", today(9)))
        val vm = started()
        assertEquals(0, vm.content.completedPercent)

        tasks.givenTasks(task("a", today(9), completed = true))
        runCurrent()

        assertEquals(100, vm.content.completedPercent)
    }

    @Test
    fun siFallaLaLectura_muestraErrorYReintentarRecupera() = runTest {
        tasks.observeFailure = IllegalStateException("sin permiso")
        val vm = started()
        assertEquals(DashboardUiState.Error, vm.uiState.value)

        tasks.observeFailure = null
        vm.onRetry()
        runCurrent()

        assertTrue(vm.uiState.value is DashboardUiState.Content)
    }

    @Test
    fun alPasarLaMedianoche_elResumenPasaAlNuevoDia() = runTest {
        tasks.givenTasks(
            task("hoy", today(9)),
            task("manana", LocalDateTime(2026, 10, 31, 9, 0)),
        )
        val vm = started()
        assertEquals(listOf("hoy"), vm.content.priorityTasks.map { it.taskId })

        clock.advanceBy(12.hours)
        advanceTimeBy(DAY_CHECK_INTERVAL)
        runCurrent()

        assertEquals(listOf("manana"), vm.content.priorityTasks.map { it.taskId })
    }
}
