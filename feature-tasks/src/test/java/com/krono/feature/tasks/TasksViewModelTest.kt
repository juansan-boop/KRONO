package com.krono.feature.tasks

import app.cash.turbine.test
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.ObserveTasksUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.UpdateTaskFieldsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias para [TasksViewModel] (F-03 CA-1 a CA-4, F-07 CA-1 a CA-3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate(2026, 10, 15)

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        viewModel = TasksViewModel(
            observeTasksUseCase = ObserveTasksUseCase(fakeRepository),
            updateTaskFieldsUseCase = UpdateTaskFieldsUseCase(fakeRepository),
            deleteTaskUseCase = DeleteTaskUseCase(fakeRepository),
        ).apply {
            currentDateProvider = { fixedToday }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alCargarTareas_agrupaEnHoyYEstaSemana_conConteos(
    ) = runTest {
        val taskHoy1 = Task(
            id = "1",
            title = "Informe de Cálculo",
            category = TaskCategory.ESTUDIO,
            priority = TaskPriority.ALTA,
            dueDate = LocalDateTime(2026, 10, 15, 18, 0),
        )
        val taskHoy2 = Task(
            id = "2",
            title = "Reunión laboral",
            category = TaskCategory.TRABAJO,
            dueDate = LocalDateTime(2026, 10, 15, 19, 0),
        )
        val taskSemana = Task(
            id = "3",
            title = "Lectura personal",
            category = TaskCategory.PERSONAL,
            dueDate = LocalDateTime(2026, 10, 18, 10, 0),
            estimatedDuration = 30.minutes,
        )

        fakeRepository.givenTasks(taskHoy1, taskHoy2, taskSemana)

        viewModel.uiState.test {
            // Estado inicial cargando
            awaitItem()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(2, state.todayTasks.size)
            assertEquals(1, state.thisWeekTasks.size)
            assertEquals(3, state.categoryCounts[TaskCategoryFilter.TODAS])
            assertEquals(1, state.categoryCounts[TaskCategoryFilter.ESTUDIO])
            assertEquals(1, state.categoryCounts[TaskCategoryFilter.TRABAJO])
            assertEquals(1, state.categoryCounts[TaskCategoryFilter.PERSONAL])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alFiltrarPorCategoria_soloMuestraTareasDeEsaCategoria() = runTest {
        val tEstudio = Task(
            id = "1",
            title = "Estudio",
            category = TaskCategory.ESTUDIO,
            dueDate = LocalDateTime(2026, 10, 15, 10, 0),
        )
        val tTrabajo = Task(
            id = "2",
            title = "Trabajo",
            category = TaskCategory.TRABAJO,
            dueDate = LocalDateTime(2026, 10, 15, 12, 0),
        )
        fakeRepository.givenTasks(tEstudio, tTrabajo)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial load con TODAS

            viewModel.onSelectCategory(TaskCategoryFilter.ESTUDIO)

            val filteredState = awaitItem()
            assertEquals(TaskCategoryFilter.ESTUDIO, filteredState.selectedCategory)
            assertEquals(1, filteredState.todayTasks.size)
            assertEquals("Estudio", filteredState.todayTasks[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alConmutarCompletitud_actualizaTareaYRecalculaEnfoqueDiario() = runTest {
        val t1 = Task(
            id = "1",
            title = "Tarea 1",
            dueDate = LocalDateTime(2026, 10, 15, 10, 0),
            isCompleted = false,
        )
        val t2 = Task(
            id = "2",
            title = "Tarea 2",
            dueDate = LocalDateTime(2026, 10, 15, 14, 0),
            isCompleted = false,
        )
        fakeRepository.givenTasks(t1, t2)

        viewModel.uiState.test {
            awaitItem() // Loading
            val initial = awaitItem()
            assertEquals(0, initial.dailyFocusPercentage)
            assertEquals(2, initial.dailyPendingCount)

            viewModel.onToggleTaskCompletion(t1)
            advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(50, updated.dailyFocusPercentage)
            assertEquals(1, updated.dailyPendingCount)

            viewModel.onToggleTaskCompletion(t2)
            advanceUntilIdle()

            val allCompleted = awaitItem()
            assertEquals(100, allCompleted.dailyFocusPercentage)
            assertEquals(0, allCompleted.dailyPendingCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cuandoNoHayTareas_emiteEstadoVacio() = runTest {
        viewModel.uiState.test {
            awaitItem() // Loading
            val emptyState = awaitItem()
            assertTrue(emptyState.isEmpty)
            assertEquals(0, emptyState.dailyFocusPercentage)
            assertEquals(0, emptyState.todayTasks.size)
            assertEquals(0, emptyState.thisWeekTasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alEliminarTarea_seRemueveDeLaLista() = runTest {
        val task = Task(id = "del", title = "Por borrar")
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            val withTask = awaitItem()
            assertEquals(1, withTask.thisWeekTasks.size)

            viewModel.onDeleteTask("del")
            advanceUntilIdle()

            val empty = awaitItem()
            assertTrue(empty.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alPedirEliminarTarea_estableceTaskPendingDeleteParaConfirmacion() = runTest {
        // F-07 CA-1: Se solicita confirmación con los datos de la tarea y subtareas
        val task = Task(id = "t1", title = "Tarea para eliminar")
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial data

            viewModel.onRequestDeleteTask(task)

            val state = awaitItem()
            assertNotNull(state.taskPendingDelete)
            assertEquals("t1", state.taskPendingDelete?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alDescartarDialogoEliminar_limpiaTaskPendingDeleteSinEliminar() = runTest {
        // F-07 CA-2: Con "Conservar tarea" no cambia nada
        val task = Task(id = "t1", title = "Conservar")
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial data

            viewModel.onRequestDeleteTask(task)
            val stateWithPending = awaitItem()
            assertEquals("t1", stateWithPending.taskPendingDelete?.id)

            viewModel.onDismissDeleteDialog()
            val stateAfterDismiss = awaitItem()
            assertNull(stateAfterDismiss.taskPendingDelete)

            val taskResult = fakeRepository.getTaskById("t1")
            assertTrue(taskResult is com.krono.core.common.KronoResult.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alConfirmarEliminacion_eliminaTareaYSubtareasConDeleteTaskUseCase() = runTest {
        // F-07 CA-2: Al elegir "Sí, eliminar tarea" se eliminan la tarea y sus subtareas
        val task = Task(id = "t1", title = "Eliminar de verdad")
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial data

            viewModel.onRequestDeleteTask(task)
            awaitItem() // Con pending delete

            viewModel.onConfirmDeleteTask()
            advanceUntilIdle()

            // La lista y el diálogo llegan por flujos distintos: se valida el estado final.
            val stateAfterDelete = expectMostRecentItem()
            assertNull(stateAfterDelete.taskPendingDelete)
            assertTrue(stateAfterDelete.isEmpty)

            val taskResult = fakeRepository.getTaskById("t1")
            assertTrue(taskResult is com.krono.core.common.KronoResult.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alPosponerParaManana_conDueDatePrevia_incrementaUnDiaYPersisteSinEliminar() = runTest {
        // F-07 CA-3: Mueve la fecha límite un día y la tarea no se elimina
        val task = Task(
            id = "t1",
            title = "Posponer",
            dueDate = LocalDateTime(2026, 10, 15, 18, 0),
        )
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial data

            viewModel.onRequestDeleteTask(task)
            awaitItem()

            viewModel.onPostponeTaskToTomorrow(task)
            advanceUntilIdle()

            val stateAfterPostpone = awaitItem()
            assertNull(stateAfterPostpone.taskPendingDelete)

            val taskResult = fakeRepository.getTaskById("t1")
            assertTrue(taskResult is com.krono.core.common.KronoResult.Success)
            val updated = (taskResult as com.krono.core.common.KronoResult.Success).data
            assertEquals(LocalDateTime(2026, 10, 16, 18, 0), updated.dueDate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alPosponerParaManana_sinDueDatePrevia_calculaFechaDeMananaDeterministaConCurrentDateProvider() = runTest {
        // F-07 CA-3: Sin dueDate previa calcula mañana con currentDateProvider determinista
        val taskSinFecha = Task(id = "t1", title = "Posponer sin fecha", dueDate = null)
        fakeRepository.givenTasks(taskSinFecha)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Initial data

            viewModel.onRequestDeleteTask(taskSinFecha)
            awaitItem()

            viewModel.onPostponeTaskToTomorrow(taskSinFecha)
            advanceUntilIdle()

            val stateAfterPostpone = awaitItem()
            assertNull(stateAfterPostpone.taskPendingDelete)

            val taskResult = fakeRepository.getTaskById("t1")
            assertTrue(taskResult is com.krono.core.common.KronoResult.Success)
            val updated = (taskResult as com.krono.core.common.KronoResult.Success).data
            // fixedToday = 2026-10-15 -> mañana = 2026-10-16 23:59
            assertEquals(LocalDateTime(2026, 10, 16, 23, 59), updated.dueDate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cuandoElListenerFalla_muestraErrorDeCargaEnVezDeEstadoVacio() = runTest {
        fakeRepository.observeFailure = TaskError.StorageError("Permiso denegado")

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.hasLoadError)
            assertFalse(state.showEmptyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alReintentarTrasUnFallo_reabreLaObservacionYMuestraLasTareas() = runTest {
        fakeRepository.observeFailure = TaskError.StorageError("Permiso denegado")
        val task = Task(id = "t1", title = "Recuperada", dueDate = LocalDateTime(2026, 10, 15, 18, 0))
        fakeRepository.givenTasks(task)

        viewModel.uiState.test {
            awaitItem() // Loading
            assertTrue(awaitItem().hasLoadError)

            fakeRepository.observeFailure = null
            viewModel.onRetryLoad()
            advanceUntilIdle()

            val recovered = expectMostRecentItem()
            assertFalse(recovered.hasLoadError)
            assertEquals(listOf(task), recovered.todayTasks)
            assertEquals(2, fakeRepository.observeTasksSubscriptions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alConmutarCompletitudConSnapshotViejo_noPisaElTiempoRealRegistrado() = runTest {
        val stale = Task(id = "t1", title = "Con cronómetro", actualDuration = Duration.ZERO)
        fakeRepository.givenTasks(stale.copy(actualDuration = 30.minutes))

        viewModel.onToggleTaskCompletion(stale)
        advanceUntilIdle()

        val stored = fakeRepository.storedTask("t1")
        assertTrue(stored?.isCompleted == true)
        assertEquals(30.minutes, stored?.actualDuration)
    }

    @Test
    fun alPosponerConSnapshotViejo_noPisaElTiempoRealRegistrado() = runTest {
        val stale = Task(id = "t1", title = "Posponer", dueDate = LocalDateTime(2026, 10, 15, 18, 0))
        fakeRepository.givenTasks(stale.copy(actualDuration = 30.minutes))

        viewModel.onPostponeTaskToTomorrow(stale)
        advanceUntilIdle()

        val stored = fakeRepository.storedTask("t1")
        assertEquals(LocalDateTime(2026, 10, 16, 18, 0), stored?.dueDate)
        assertEquals(30.minutes, stored?.actualDuration)
    }

    @Test
    fun alConfirmarEliminacionDosVeces_soloEliminaUnaVez() = runTest {
        val task = Task(id = "t1", title = "Doble toque")
        fakeRepository.givenTasks(task)
        viewModel.onRequestDeleteTask(task)

        viewModel.onConfirmDeleteTask()
        viewModel.onConfirmDeleteTask()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.deleteCalls)
    }

    private fun diaDeDemostracion(): List<Task> = listOf(
        Task(id = "hoy-hecha", title = "Hoy hecha", category = TaskCategory.ESTUDIO, isCompleted = true, dueDate = LocalDateTime(2026, 10, 15, 9, 0)),
        Task(id = "hoy-pend-1", title = "Hoy pendiente 1", category = TaskCategory.TRABAJO, dueDate = LocalDateTime(2026, 10, 15, 20, 0)),
        Task(id = "hoy-pend-2", title = "Hoy pendiente 2", category = TaskCategory.PERSONAL, dueDate = LocalDateTime(2026, 10, 15, 21, 0)),
        Task(id = "pasada-hecha-1", title = "Pasada hecha 1", category = TaskCategory.ESTUDIO, isCompleted = true, dueDate = LocalDateTime(2026, 10, 10, 9, 0)),
        Task(id = "pasada-hecha-2", title = "Pasada hecha 2", category = TaskCategory.TRABAJO, isCompleted = true, dueDate = LocalDateTime(2026, 10, 14, 9, 0)),
        Task(id = "vencida", title = "Vencida pendiente", category = TaskCategory.ESTUDIO, dueDate = LocalDateTime(2026, 10, 13, 9, 0)),
        Task(id = "futura", title = "Futura", category = TaskCategory.ESTUDIO, dueDate = LocalDateTime(2026, 10, 17, 9, 0)),
    )

    @Test
    fun lasCompletadasDeDiasAnteriores_noInflanParaHoy_yVanAlGrupoDeCompletadas() = runTest {
        fakeRepository.givenTasks(*diaDeDemostracion().toTypedArray())

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(setOf("hoy-hecha", "hoy-pend-1", "hoy-pend-2", "vencida"), state.todayTasks.map { it.id }.toSet())
            assertEquals(listOf("futura"), state.thisWeekTasks.map { it.id })
            assertEquals(setOf("pasada-hecha-1", "pasada-hecha-2"), state.completedEarlierTasks.map { it.id }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lasVencidasPendientes_quedanEnParaHoyAntesQueLasDeHoy() = runTest {
        fakeRepository.givenTasks(*diaDeDemostracion().toTypedArray())

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals("vencida", state.todayTasks.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun enfoqueDiario_usaElMismoCalculoQueInicio_ignorandoVencidasYCompletadasPasadas() = runTest {
        fakeRepository.givenTasks(*diaDeDemostracion().toTypedArray())

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            // Hoy vencen 3 tareas, 1 completada: 33 % y 2 pendientes, igual que el resumen de Inicio.
            assertEquals(33, state.dailyFocusPercentage)
            assertEquals(2, state.dailyPendingCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun enfoqueDiario_noCambiaAlFiltrarPorCategoria() = runTest {
        fakeRepository.givenTasks(*diaDeDemostracion().toTypedArray())

        viewModel.uiState.test {
            awaitItem()
            val todas = awaitItem()
            viewModel.onSelectCategory(TaskCategoryFilter.PERSONAL)
            val personal = awaitItem()
            assertEquals(todas.dailyFocusPercentage, personal.dailyFocusPercentage)
            assertEquals(todas.dailyPendingCount, personal.dailyPendingCount)
            assertEquals(listOf("hoy-pend-2"), personal.todayTasks.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
