package com.krono.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.UpdateTaskFieldsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias para [TaskDetailViewModel] cubriendo todos los criterios de F-06 y F-07 (docs/SPEC.md).
 *
 * - F-06 CA-1: Precarga de atributos, cálculo determinista de tiempo restante ("En 3 horas") y progreso x/n.
 * - F-06 CA-2: Conmutación y adición de subtareas con actualización reactiva y persistencia.
 * - F-06 CA-3: Acción "Iniciar Enfoque" y emisión de evento hacia F-09.
 * - F-07 CA-1: Confirmación de eliminación con conteo de subtareas.
 * - F-07 CA-2: Eliminación persistente con DeleteTaskUseCase y conservación con descarte.
 * - F-07 CA-3: Posponer para mañana sumando 1 día deterministamente con UpdateTaskFieldsUseCase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedNow = LocalDateTime(2026, 10, 24, 15, 0)

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var observeTaskByIdUseCase: ObserveTaskByIdUseCase
    private lateinit var updateTaskFieldsUseCase: UpdateTaskFieldsUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    private val sampleTask = Task(
        id = "task-100",
        title = "Entrega Informe Trimestral",
        category = TaskCategory.TRABAJO,
        priority = TaskPriority.ALTA,
        dueDate = LocalDateTime(2026, 10, 24, 18, 0),
        estimatedDuration = 45.minutes,
        actualDuration = 20.minutes,
        isCompleted = false,
        note = "Incluir métricas de ventas de agosto",
        subtasks = listOf(
            Subtask(id = "sub-1", taskId = "task-100", title = "Paso 1: Datos", isDone = true, position = 0),
            Subtask(id = "sub-2", taskId = "task-100", title = "Paso 2: Gráficos", isDone = true, position = 1),
            Subtask(id = "sub-3", taskId = "task-100", title = "Paso 3: Conclusiones", isDone = false, position = 2),
            Subtask(id = "sub-4", taskId = "task-100", title = "Paso 4: Envío", isDone = false, position = 3),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        observeTaskByIdUseCase = ObserveTaskByIdUseCase(fakeRepository)
        updateTaskFieldsUseCase = UpdateTaskFieldsUseCase(fakeRepository)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(taskId: String?): TaskDetailViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (taskId != null) {
                set(TaskDetailViewModel.TASK_ID_NAV_ARG, taskId)
            }
        }
        return TaskDetailViewModel(
            savedStateHandle = savedStateHandle,
            observeTaskByIdUseCase = observeTaskByIdUseCase,
            updateTaskFieldsUseCase = updateTaskFieldsUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
        ).apply {
            currentDateTimeProvider = { fixedNow }
        }
    }

    @Test
    fun alIniciarConTaskIdExistente_cargaAtributosTiempoRestanteYProgreso() = runTest {
        // CA-1: Categoría, prioridad, límite de entrega con tiempo restante ("En 3 horas"), estimación, progreso (2/4) y nota
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isTaskNotFound)
        assertNotNull(state.task)

        val task = state.task!!
        assertEquals("Entrega Informe Trimestral", task.title)
        assertEquals(TaskCategory.TRABAJO, task.category)
        assertEquals(TaskPriority.ALTA, task.priority)
        assertEquals(45.minutes, task.estimatedDuration)
        assertEquals("Incluir métricas de ventas de agosto", task.note)

        // Verificación de tiempo restante ("En 3 horas": de 15:00 a 18:00)
        assertEquals(RemainingTime.InHours(3), state.remainingTime)

        // Verificación de progreso (2/4) y 50%
        assertEquals(4, state.totalSubtasksCount)
        assertEquals(2, state.completedSubtasksCount)
        assertEquals(0.5f, state.progressRatio, 0.001f)
    }

    @Test
    fun calculoDeTiempoRestante_evaluaMinutosHorasDiasYVencida() = runTest {
        val viewModel = createViewModel(null)

        // En 45 minutos (15:45 vs now 15:00)
        val dueInMinutes = LocalDateTime(2026, 10, 24, 15, 45)
        assertEquals(RemainingTime.InMinutes(45), viewModel.calculateRemainingTime(dueInMinutes, fixedNow))

        // En 3 horas (18:00 vs now 15:00)
        val dueInHours = LocalDateTime(2026, 10, 24, 18, 0)
        assertEquals(RemainingTime.InHours(3), viewModel.calculateRemainingTime(dueInHours, fixedNow))

        // En 2 días (26 Octubre 15:00 vs 24 Octubre 15:00)
        val dueInDays = LocalDateTime(2026, 10, 26, 15, 0)
        assertEquals(RemainingTime.InDays(2), viewModel.calculateRemainingTime(dueInDays, fixedNow))

        // Vencida (14:00 vs now 15:00)
        val dueInPast = LocalDateTime(2026, 10, 24, 14, 0)
        assertEquals(RemainingTime.Overdue, viewModel.calculateRemainingTime(dueInPast, fixedNow))

        // Sin fecha
        assertNull(viewModel.calculateRemainingTime(null, fixedNow))
    }

    @Test
    fun alConmutarSubtarea_actualizaEstadoYPersisteEnRepositorio() = runTest {
        // CA-2: Marcar y desmarcar subtarea actualiza progreso y persiste
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        // Marcamos sub-3 (que estaba en false -> pasa a true)
        viewModel.onToggleSubtask("sub-3")
        advanceUntilIdle()

        val updatedTaskResult = fakeRepository.getTaskById("task-100")
        assertTrue(updatedTaskResult is com.krono.core.common.KronoResult.Success)
        val updatedTask = (updatedTaskResult as com.krono.core.common.KronoResult.Success).data

        val toggledSubtask = updatedTask.subtasks.first { it.id == "sub-3" }
        assertTrue(toggledSubtask.isDone)

        // Progreso se actualiza a 3/4 (75%)
        val state = viewModel.uiState.value
        assertEquals(3, state.completedSubtasksCount)
        assertEquals(4, state.totalSubtasksCount)
        assertEquals(0.75f, state.progressRatio, 0.001f)

        // Desmarcamos sub-1 (que estaba en true -> pasa a false)
        viewModel.onToggleSubtask("sub-1")
        advanceUntilIdle()

        val stateAfterUncheck = viewModel.uiState.value
        assertEquals(2, stateAfterUncheck.completedSubtasksCount)
        assertEquals(0.5f, stateAfterUncheck.progressRatio, 0.001f)
    }

    @Test
    fun alAnadirSubtarea_agregaAListaYPersisteConNuevoProgreso() = runTest {
        // CA-2: Agregar subtarea actualiza progreso y persiste
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onOpenAddSubtaskDialog()
        assertTrue(viewModel.uiState.value.isAddingSubtask)

        viewModel.onAddSubtask("Paso 5: Revisión del tutor")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddingSubtask)

        val updatedTaskResult = fakeRepository.getTaskById("task-100")
        assertTrue(updatedTaskResult is com.krono.core.common.KronoResult.Success)
        val updatedTask = (updatedTaskResult as com.krono.core.common.KronoResult.Success).data

        assertEquals(5, updatedTask.subtasks.size)
        val lastSubtask = updatedTask.subtasks.last()
        assertEquals("Paso 5: Revisión del tutor", lastSubtask.title)
        assertFalse(lastSubtask.isDone)
        assertEquals(4, lastSubtask.position)

        val state = viewModel.uiState.value
        assertEquals(5, state.totalSubtasksCount)
        assertEquals(2, state.completedSubtasksCount)
        assertEquals(0.4f, state.progressRatio, 0.001f)
    }

    @Test
    fun alTocarIniciarEnfoque_emiteEventoNavigateToFocus() = runTest {
        // CA-3: Tocar Iniciar Enfoque emite evento hacia F-09
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onStartFocusClicked()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TaskDetailEvent.NavigateToFocus)
            assertEquals("task-100", (event as TaskDetailEvent.NavigateToFocus).taskId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alTocarEditar_emiteEventoNavigateToEdit() = runTest {
        fakeRepository.givenTasks(sampleTask)

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onEditTaskClicked()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TaskDetailEvent.NavigateToEdit)
            assertEquals("task-100", (event as TaskDetailEvent.NavigateToEdit).taskId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alIniciarConTaskIdInexistente_marcaComoNoEncontrada() = runTest {
        val viewModel = createViewModel("task-inexistente")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isTaskNotFound)
        assertNull(state.task)
    }

    @Test
    fun alPedirEliminarTarea_activaConfirmacionDeEliminacion() = runTest {
        // F-07 CA-1: Se muestra confirmación al solicitar eliminar
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onDeleteTaskClicked()

        assertTrue(viewModel.uiState.value.isConfirmingDelete)
    }

    @Test
    fun alDescartarDialogoEliminar_desactivaConfirmacionSinEliminar() = runTest {
        // F-07 CA-2: Con "Conservar tarea" no cambia nada
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onDeleteTaskClicked()
        assertTrue(viewModel.uiState.value.isConfirmingDelete)

        viewModel.onDismissDeleteDialog()
        assertFalse(viewModel.uiState.value.isConfirmingDelete)

        val taskResult = fakeRepository.getTaskById("task-100")
        assertTrue(taskResult is com.krono.core.common.KronoResult.Success)
    }

    @Test
    fun alConfirmarEliminarTarea_eliminaYEmiteNavigateBack() = runTest {
        // F-07 CA-2: Al elegir "Sí, eliminar tarea" se elimina la tarea y sus subtareas
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onDeleteTaskClicked()
            viewModel.onConfirmDeleteTask()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TaskDetailEvent.NavigateBack)

            val taskResult = fakeRepository.getTaskById("task-100")
            assertTrue(taskResult is com.krono.core.common.KronoResult.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alPosponerParaManana_incrementaUnDiaYPersisteSinEliminar() = runTest {
        // F-07 CA-3: Mueve la fecha límite un día y la tarea no se elimina
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onDeleteTaskClicked()
        viewModel.onPostponeTaskToTomorrow()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isConfirmingDelete)

        val taskResult = fakeRepository.getTaskById("task-100")
        assertTrue(taskResult is com.krono.core.common.KronoResult.Success)
        val updatedTask = (taskResult as com.krono.core.common.KronoResult.Success).data
        // La fecha original era 2026-10-24 18:00 -> pasa a 2026-10-25 18:00
        assertEquals(LocalDateTime(2026, 10, 25, 18, 0), updatedTask.dueDate)
    }

    @Test
    fun alPosponerParaManana_sinDueDatePrevia_asignaFechaDeMananaDeterminista() = runTest {
        // F-07 CA-3: Sin dueDate previa calcula mañana con currentDateTimeProvider
        val taskSinFecha = sampleTask.copy(id = "task-sin-fecha", dueDate = null)
        fakeRepository.givenTasks(taskSinFecha)
        val viewModel = createViewModel("task-sin-fecha")
        advanceUntilIdle()

        viewModel.onPostponeTaskToTomorrow()
        advanceUntilIdle()

        val taskResult = fakeRepository.getTaskById("task-sin-fecha")
        assertTrue(taskResult is com.krono.core.common.KronoResult.Success)
        val updatedTask = (taskResult as com.krono.core.common.KronoResult.Success).data
        // fixedNow = 2026-10-24 15:00 -> mañana es 2026-10-25 23:59
        assertEquals(LocalDateTime(2026, 10, 25, 23, 59), updatedTask.dueDate)
    }

    @Test
    fun alConmutarSubtarea_soloEscribeLasSubtareas() = runTest {
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onToggleSubtask("sub-3")
        advanceUntilIdle()

        assertEquals(setOf(TaskField.SUBTASKS), fakeRepository.lastUpdatedFields)
    }

    @Test
    fun alPosponer_soloEscribeLaFechaLimite() = runTest {
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onPostponeTaskToTomorrow()
        advanceUntilIdle()

        assertEquals(setOf(TaskField.DUE_DATE), fakeRepository.lastUpdatedFields)
    }

    @Test
    fun alPosponerDosVecesSeguidas_soloEscribeUnaVez() = runTest {
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.onDeleteTaskClicked()
        viewModel.onPostponeTaskToTomorrow()
        viewModel.onPostponeTaskToTomorrow()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.updateFieldsCalls)
        assertEquals(LocalDateTime(2026, 10, 25, 18, 0), fakeRepository.storedTask("task-100")?.dueDate)
    }

    @Test
    fun alConfirmarEliminarDosVeces_eliminaUnaVezYNavegaUnaVez() = runTest {
        fakeRepository.givenTasks(sampleTask)
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onDeleteTaskClicked()
            viewModel.onConfirmDeleteTask()
            viewModel.onConfirmDeleteTask()
            advanceUntilIdle()

            assertTrue(awaitItem() is TaskDetailEvent.NavigateBack)
            expectNoEvents()
        }
        assertEquals(1, fakeRepository.deleteCalls)
    }

    @Test
    fun cuandoElListenerFalla_muestraErrorDeCargaEnVezDeTareaNoEncontrada() = runTest {
        fakeRepository.givenTasks(sampleTask)
        fakeRepository.observeFailure = TaskError.StorageError("Permiso denegado")

        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasLoadError)
        assertFalse(state.isTaskNotFound)
    }

    @Test
    fun alReintentarTrasUnFallo_reabreLaObservacionYMuestraLaTarea() = runTest {
        fakeRepository.givenTasks(sampleTask)
        fakeRepository.observeFailure = TaskError.StorageError("Permiso denegado")
        val viewModel = createViewModel("task-100")
        advanceUntilIdle()

        fakeRepository.observeFailure = null
        viewModel.onRetryLoad()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasLoadError)
        assertEquals(sampleTask, state.task)
        assertEquals(2, fakeRepository.observeTaskByIdSubscriptions)
    }
}
