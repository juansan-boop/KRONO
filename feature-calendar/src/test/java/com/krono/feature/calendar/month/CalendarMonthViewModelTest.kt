package com.krono.feature.calendar.month

import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.calendar.ObserveCalendarUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.TaskRepository
import com.krono.feature.calendar.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [CalendarMonthViewModel]: vista mensual y agenda del día (F-16 CA-1, CA-2 y CA-3). */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarMonthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTaskRepository()
    private val clock = FixedClock(NOW)

    private fun viewModel(taskRepository: TaskRepository = repository) = CalendarMonthViewModel(
        observeCalendar = ObserveCalendarUseCase(taskRepository),
        dateTimeProvider = CurrentDateTimeProvider(clock) { TimeZone.UTC },
    )

    /** Mantiene suscrito el estado (WhileSubscribed) y deja correr las corrutinas pendientes. */
    private fun TestScope.started(vm: CalendarMonthViewModel): CalendarMonthViewModel {
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    private val CalendarMonthViewModel.state get() = uiState.value

    private val CalendarMonthUiState.agendaIds: List<String>
        get() = (agenda as DayAgendaState.Loaded).items.map { it.taskId }

    private fun CalendarMonthUiState.day(date: LocalDate): CalendarDayUi = weeks.flatten().first { it.date == date }

    @Test
    fun alAbrir_muestraElMesDeHoyConHoySeleccionado() = runTest {
        val vm = started(viewModel())

        assertEquals(TODAY, vm.state.selectedDate)
        assertEquals(TODAY, vm.state.today)
        assertEquals(2024, vm.state.displayedMonth.year)
        assertEquals(10, vm.state.displayedMonth.monthNumber)
        // Octubre de 2024: rejilla del lunes 30 de septiembre al domingo 3 de noviembre.
        assertEquals(LocalDate(2024, 9, 30), vm.state.weeks.first().first().date)
        assertEquals(LocalDate(2024, 11, 3), vm.state.weeks.last().last().date)
    }

    @Test
    fun mientrasSeLeenLasTareas_laAgendaEstaCargandoYLosDiasNoAnuncianConteo() = runTest {
        val vm = started(viewModel(ControlledTaskRepository()))

        assertEquals(DayAgendaState.Loading, vm.state.agenda)
        assertNull(vm.state.day(TODAY).taskCount)
    }

    @Test
    fun alCargar_laAgendaListaLasTareasDelDiaSeleccionadoEnOrdenConSuHorario() = runTest {
        repository.givenTasks(
            task("clase", LocalDateTime(2024, 10, 24, 16, 30), 90.minutes, TaskCategory.ESTUDIO),
            task("soporte", LocalDateTime(2024, 10, 24, 13, 0), 4.hours, TaskCategory.TRABAJO),
            task("otro-dia", LocalDateTime(2024, 10, 25, 10, 0), 1.hours),
        )

        val vm = started(viewModel())

        val items = (vm.state.agenda as DayAgendaState.Loaded).items
        assertEquals(listOf("soporte", "clase"), items.map { it.taskId })
        assertEquals(LocalDateTime(2024, 10, 24, 9, 0), items.first().start)
        assertEquals(LocalDateTime(2024, 10, 24, 13, 0), items.first().end)
        assertEquals(TaskCategory.TRABAJO, items.first().category)
    }

    @Test
    fun enCurso_soloSiElIntervaloIncluyeAhoraYLaTareaNoEstaCompletada() = runTest {
        repository.givenTasks(
            task("en-curso", LocalDateTime(2024, 10, 24, 15, 0), 2.hours),
            task("hecha", LocalDateTime(2024, 10, 24, 14, 30), 1.hours).copy(isCompleted = true),
            task("despues", LocalDateTime(2024, 10, 24, 18, 0), 1.hours),
        )

        val vm = started(viewModel())

        val statusById = (vm.state.agenda as DayAgendaState.Loaded).items.associate { it.taskId to it.status }
        assertEquals(AgendaItemStatus.IN_PROGRESS, statusById["en-curso"])
        assertEquals(AgendaItemStatus.COMPLETED, statusById["hecha"])
        assertEquals(AgendaItemStatus.SCHEDULED, statusById["despues"])
    }

    @Test
    fun alTocarUnDia_laAgendaMuestraLasTareasDeEseDia() = runTest {
        repository.givenTasks(
            task("hoy", LocalDateTime(2024, 10, 24, 10, 0)),
            task("martes", LocalDateTime(2024, 10, 29, 10, 0)),
        )
        val vm = started(viewModel())

        vm.onSelectDate(LocalDate(2024, 10, 29))
        advanceUntilIdle()

        assertEquals(LocalDate(2024, 10, 29), vm.state.selectedDate)
        assertEquals(listOf("martes"), vm.state.agendaIds)
        assertTrue(vm.state.day(LocalDate(2024, 10, 29)).isSelected)
        assertFalse(vm.state.day(TODAY).isSelected)
    }

    @Test
    fun diaSinTareas_laAgendaQuedaCargadaYVacia() = runTest {
        repository.givenTasks(task("hoy", LocalDateTime(2024, 10, 24, 10, 0)))
        val vm = started(viewModel())

        vm.onSelectDate(LocalDate(2024, 10, 26))
        advanceUntilIdle()

        assertEquals(DayAgendaState.Loaded(emptyList()), vm.state.agenda)
        assertEquals(0, vm.state.day(LocalDate(2024, 10, 26)).taskCount)
    }

    @Test
    fun rejilla_marcaHoyAtenuaVecinosYCuentaLasTareasConSusCategorias() = runTest {
        repository.givenTasks(
            task("a", LocalDateTime(2024, 10, 24, 10, 0), category = TaskCategory.ESTUDIO),
            task("b", LocalDateTime(2024, 10, 24, 12, 0), category = TaskCategory.ESTUDIO),
            task("c", LocalDateTime(2024, 10, 24, 16, 0), category = TaskCategory.TRABAJO),
        )

        val vm = started(viewModel())

        val today = vm.state.day(TODAY)
        assertTrue(today.isToday)
        assertTrue(today.isSelected)
        assertEquals(3, today.taskCount)
        assertEquals(listOf(TaskCategory.ESTUDIO, TaskCategory.TRABAJO), today.categories)
        assertFalse(vm.state.day(LocalDate(2024, 9, 30)).isInDisplayedMonth)
        assertTrue(vm.state.day(LocalDate(2024, 10, 1)).isInDisplayedMonth)
        assertFalse(vm.state.day(LocalDate(2024, 10, 1)).isToday)
    }

    @Test
    fun mesSiguiente_muestraNoviembreYSeleccionaSuDia1() = runTest {
        repository.givenTasks(task("nov", LocalDateTime(2024, 11, 1, 9, 0)))
        val vm = started(viewModel())

        vm.onNextMonth()
        advanceUntilIdle()

        assertEquals(11, vm.state.displayedMonth.monthNumber)
        assertEquals(LocalDate(2024, 11, 1), vm.state.selectedDate)
        assertEquals(listOf("nov"), vm.state.agendaIds)
        assertTrue(vm.state.day(LocalDate(2024, 11, 30)).isInDisplayedMonth)
    }

    @Test
    fun mesAnterior_muestraSeptiembreYSeleccionaSuDia1() = runTest {
        val vm = started(viewModel())

        vm.onPreviousMonth()
        advanceUntilIdle()

        assertEquals(9, vm.state.displayedMonth.monthNumber)
        assertEquals(LocalDate(2024, 9, 1), vm.state.selectedDate)
    }

    @Test
    fun alVolverAlMesActual_seSeleccionaHoy() = runTest {
        val vm = started(viewModel())

        vm.onNextMonth()
        vm.onPreviousMonth()
        advanceUntilIdle()

        assertEquals(TODAY, vm.state.selectedDate)
    }

    @Test
    fun hoy_vuelveAlMesYAlDiaActualesDesdeOtroMes() = runTest {
        repository.givenTasks(task("hoy", LocalDateTime(2024, 10, 24, 10, 0)))
        val vm = started(viewModel())
        vm.onNextMonth()
        vm.onNextMonth()
        vm.onSelectDate(LocalDate(2024, 12, 10))
        advanceUntilIdle()

        vm.onToday()
        advanceUntilIdle()

        assertEquals(10, vm.state.displayedMonth.monthNumber)
        assertEquals(TODAY, vm.state.selectedDate)
        assertEquals(listOf("hoy"), vm.state.agendaIds)
    }

    @Test
    fun hoy_usaLaFechaActualDelRelojAlPulsarse() = runTest {
        val vm = started(viewModel())
        clock.current = LocalDateTime(2024, 11, 5, 8, 0)

        vm.onToday()
        advanceUntilIdle()

        assertEquals(LocalDate(2024, 11, 5), vm.state.selectedDate)
        assertEquals(LocalDate(2024, 11, 5), vm.state.today)
        assertEquals(11, vm.state.displayedMonth.monthNumber)
    }

    @Test
    fun tocarUnDiaDeUnMesVecino_cambiaAEseMesYLoSelecciona() = runTest {
        repository.givenTasks(task("sep", LocalDateTime(2024, 9, 30, 10, 0)))
        val vm = started(viewModel())

        vm.onSelectDate(LocalDate(2024, 9, 30))
        advanceUntilIdle()

        assertEquals(9, vm.state.displayedMonth.monthNumber)
        assertEquals(LocalDate(2024, 9, 30), vm.state.selectedDate)
        assertEquals(listOf("sep"), vm.state.agendaIds)
    }

    @Test
    fun errorDelDominio_muestraErrorSinAgendaVaciaFalsaNiConteos() = runTest {
        repository.observeFailure = IllegalStateException("PERMISSION_DENIED")

        val vm = started(viewModel())

        assertEquals(DayAgendaState.LoadError, vm.state.agenda)
        assertTrue(vm.state.weeks.flatten().all { it.taskCount == null && it.categories.isEmpty() })
    }

    @Test
    fun reintentar_trasUnError_vuelveAObservarYMuestraLasTareas() = runTest {
        repository.observeFailure = IllegalStateException("PERMISSION_DENIED")
        repository.givenTasks(task("hoy", LocalDateTime(2024, 10, 24, 10, 0)))
        val vm = started(viewModel())

        repository.observeFailure = null
        vm.onRetry()
        advanceUntilIdle()

        assertEquals(listOf("hoy"), vm.state.agendaIds)
        assertEquals(2, repository.observeTasksSubscriptions)
    }

    @Test
    fun cambiarDeDia_noVuelveAAbrirLaObservacionDeTareas() = runTest {
        val vm = started(viewModel())

        vm.onSelectDate(LocalDate(2024, 10, 2))
        vm.onSelectDate(LocalDate(2024, 10, 3))
        advanceUntilIdle()

        assertEquals(1, repository.observeTasksSubscriptions)
    }

    @Test
    fun alCambiarLasTareas_laAgendaSeActualiza() = runTest {
        val controlled = ControlledTaskRepository()
        val vm = started(viewModel(controlled))
        controlled.emit(KronoResult.Success(emptyList()))
        advanceUntilIdle()
        assertEquals(DayAgendaState.Loaded(emptyList()), vm.state.agenda)

        controlled.emit(KronoResult.Success(listOf(task("nueva", LocalDateTime(2024, 10, 24, 20, 0)))))
        advanceUntilIdle()

        assertEquals(listOf("nueva"), vm.state.agendaIds)
    }

    private fun task(
        id: String,
        due: LocalDateTime,
        estimate: Duration? = null,
        category: TaskCategory = TaskCategory.PERSONAL,
        priority: TaskPriority = TaskPriority.MEDIA,
    ) = Task(id = id, title = id, category = category, priority = priority, dueDate = due, estimatedDuration = estimate)

    private companion object {
        /** Jueves 24 de octubre de 2024 a las 14:00, el día del diseño de referencia. */
        val NOW = LocalDateTime(2024, 10, 24, 14, 0)
        val TODAY: LocalDate = NOW.date
    }
}
