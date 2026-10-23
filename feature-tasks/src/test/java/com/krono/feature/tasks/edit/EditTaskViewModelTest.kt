package com.krono.feature.tasks.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.GetTaskByIdUseCase
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.UpsertTaskUseCase
import com.krono.feature.tasks.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias para [EditTaskViewModel] que validan la funcionalidad F-05 (Editar tarea).
 *
 * Cubre:
 * - CA-1: Precarga de tarea existente con sus valores actuales.
 * - CA-2: Actualización persistida preservando metadatos no editables (id, isCompleted, actualDuration, subtasks, note).
 * - CA-3: Validaciones obligatorias de título, fecha y no retroactividad temporal.
 * - Caso borde: Identificador no encontrado en el repositorio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditTaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedNow = LocalDateTime(2026, 10, 14, 12, 0)

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase
    private lateinit var upsertTaskUseCase: UpsertTaskUseCase

    private val sampleTask = Task(
        id = "task-123",
        title = "Entrega Informe Trimestral",
        category = TaskCategory.TRABAJO,
        priority = TaskPriority.ALTA,
        dueDate = LocalDateTime(2026, 10, 24, 18, 0),
        estimatedDuration = 45.minutes,
        actualDuration = 30.minutes,
        isCompleted = true,
        note = "Revisar sección de métricas clave",
        subtasks = listOf(
            Subtask(id = "sub-1", taskId = "task-123", title = "Paso 1: Datos", isDone = true, position = 0),
            Subtask(id = "sub-2", taskId = "task-123", title = "Paso 2: Gráficos", isDone = false, position = 1),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        getTaskByIdUseCase = GetTaskByIdUseCase(fakeRepository)
        upsertTaskUseCase = UpsertTaskUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(taskId: String?): EditTaskViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (taskId != null) {
                set(EditTaskViewModel.TASK_ID_NAV_ARG, taskId)
            }
        }
        return EditTaskViewModel(
            savedStateHandle = savedStateHandle,
            getTaskByIdUseCase = getTaskByIdUseCase,
            upsertTaskUseCase = upsertTaskUseCase,
        ).apply {
            currentDateTimeProvider = { fixedNow }
        }
    }

    @Test
    fun alIniciarConTaskIdExistente_precargaFormularioConValoresActuales() = runTest {
        // CA-1: Precarga de tarea existente
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isTaskNotFound)
        assertEquals("task-123", state.taskId)
        assertEquals("Entrega Informe Trimestral", state.title)
        assertEquals(TaskCategory.TRABAJO, state.category)
        assertEquals(TaskPriority.ALTA, state.priority)
        assertEquals(LocalDate(2026, 10, 24), state.dueDate)
        assertEquals(LocalTime(18, 0), state.dueTime)
        assertEquals(45.minutes, state.estimatedDuration)
        assertNull(state.titleErrorRes)
        assertNull(state.dateErrorRes)
        assertNull(state.dateTimeErrorRes)
    }

    @Test
    fun alGuardarCambiosValidos_actualizaCamposEditablesYPreservaMetadatosNoEditables() = runTest {
        // CA-2: Actualización preservando id, isCompleted, actualDuration, subtasks, note
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-123")
        advanceUntilIdle()

        // Modificaciones válidas en campos editables
        viewModel.onTitleChanged("Entrega Informe Trimestral - Final")
        viewModel.onCategorySelected(TaskCategory.ESTUDIO)
        viewModel.onPrioritySelected(TaskPriority.MEDIA)
        viewModel.onDateSelected(LocalDate(2026, 10, 25))
        viewModel.onTimeSelected(LocalTime(20, 0))
        viewModel.onDurationSelected(60.minutes)

        viewModel.events.test {
            viewModel.saveChanges()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is EditTaskEvent.NavigateBack)

            val updatedResult = fakeRepository.getTaskById("task-123")
            assertTrue(updatedResult is com.krono.core.common.KronoResult.Success)
            val updatedTask = (updatedResult as com.krono.core.common.KronoResult.Success).data

            // Campos actualizados
            assertEquals("Entrega Informe Trimestral - Final", updatedTask.title)
            assertEquals(TaskCategory.ESTUDIO, updatedTask.category)
            assertEquals(TaskPriority.MEDIA, updatedTask.priority)
            assertEquals(LocalDateTime(2026, 10, 25, 20, 0), updatedTask.dueDate)
            assertEquals(60.minutes, updatedTask.estimatedDuration)

            // Metadatos preservados estrictamente (CA-2)
            assertEquals("task-123", updatedTask.id)
            assertTrue(updatedTask.isCompleted)
            assertEquals(30.minutes, updatedTask.actualDuration)
            assertEquals("Revisar sección de métricas clave", updatedTask.note)
            assertEquals(2, updatedTask.subtasks.size)
            assertEquals("sub-1", updatedTask.subtasks[0].id)
            assertEquals("sub-2", updatedTask.subtasks[1].id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alGuardarConTituloEnBlanco_muestraErrorYNoPersiste() = runTest {
        // CA-3: Título obligatorio
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-123")
        advanceUntilIdle()

        viewModel.onTitleChanged("   ")
        viewModel.saveChanges()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_title_required, state.titleErrorRes)
        assertTrue(state.isTitleError)
        assertEquals(0, fakeRepository.upsertCalls)
    }

    @Test
    fun alGuardarTareaSinFecha_muestraErrorYNoPersiste() = runTest {
        // CA-3: Fecha obligatoria
        val taskWithoutDueDate = sampleTask.copy(id = "task-no-date", dueDate = null)
        fakeRepository.givenTasks(taskWithoutDueDate)

        val viewModel = createViewModel("task-no-date")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dueDate)
        viewModel.saveChanges()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_date_required, state.dateErrorRes)
        assertTrue(state.isDateError)
        assertEquals(0, fakeRepository.upsertCalls)
    }

    @Test
    fun alGuardarConFechaHoraEnElPasado_muestraErrorYNoPersiste() = runTest {
        // CA-3: Fecha y hora no pueden ser retroactivas (fixedNow: 2026-10-14 12:00)
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-123")
        advanceUntilIdle()

        viewModel.onDateSelected(LocalDate(2026, 10, 14))
        viewModel.onTimeSelected(LocalTime(11, 0)) // 1 hora antes de fixedNow
        viewModel.saveChanges()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_due_date_in_past, state.dateTimeErrorRes)
        assertTrue(state.isTimeError)
        assertEquals(0, fakeRepository.upsertCalls)
    }

    @Test
    fun alIniciarConTaskIdInexistente_marcaTareaComoNoEncontrada() = runTest {
        // Caso borde: tarea inexistente en el repositorio
        val viewModel = createViewModel("task-inexistente")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isTaskNotFound)
    }

    @Test
    fun alModificarTituloConErrorPrevio_limpiaElError() = runTest {
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-123")
        advanceUntilIdle()

        viewModel.onTitleChanged("")
        viewModel.saveChanges()
        assertTrue(viewModel.uiState.value.isTitleError)

        viewModel.onTitleChanged("Título Corregido")
        assertFalse(viewModel.uiState.value.isTitleError)
        assertNull(viewModel.uiState.value.titleErrorRes)
    }
}
