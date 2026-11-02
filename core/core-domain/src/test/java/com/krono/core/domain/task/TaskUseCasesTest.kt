package com.krono.core.domain.task

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias para los casos de uso de tareas en core-domain.
 */
class TaskUseCasesTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var observeTasksUseCase: ObserveTasksUseCase
    private lateinit var observeTaskByIdUseCase: ObserveTaskByIdUseCase
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase
    private lateinit var upsertTaskUseCase: UpsertTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var updateTaskFieldsUseCase: UpdateTaskFieldsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeTaskRepository()
        observeTasksUseCase = ObserveTasksUseCase(fakeRepository)
        observeTaskByIdUseCase = ObserveTaskByIdUseCase(fakeRepository)
        getTaskByIdUseCase = GetTaskByIdUseCase(fakeRepository)
        upsertTaskUseCase = UpsertTaskUseCase(fakeRepository)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository)
        updateTaskFieldsUseCase = UpdateTaskFieldsUseCase(fakeRepository)
    }

    @Test
    fun observeTasks_emiteListaDeTareas() = runTest {
        val task1 = Task(id = "1", title = "Tarea 1", category = TaskCategory.ESTUDIO)
        val task2 = Task(id = "2", title = "Tarea 2", category = TaskCategory.TRABAJO)
        fakeRepository.givenTasks(task1, task2)

        observeTasksUseCase().test {
            val items = (awaitItem() as KronoResult.Success).data
            assertEquals(2, items.size)
            assertEquals("Tarea 1", items[0].title)
            assertEquals("Tarea 2", items[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTaskById_emiteTareaCorrespondiente_oNuloSiNoExiste() = runTest {
        val task = Task(id = "1", title = "Tarea 1")
        fakeRepository.givenTasks(task)

        observeTaskByIdUseCase("1").test {
            assertEquals("Tarea 1", (awaitItem() as KronoResult.Success).data?.title)
            cancelAndIgnoreRemainingEvents()
        }

        observeTaskByIdUseCase("inexistente").test {
            assertEquals(KronoResult.Success(null), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTasks_cuandoElListenerFalla_emiteErrorEnVezDeListaVacia() = runTest {
        val listenerError = TaskError.StorageError("Permiso denegado")
        fakeRepository.observeFailure = listenerError

        observeTasksUseCase().test {
            assertEquals(KronoResult.Error(listenerError), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTaskById_cuandoExiste_devuelveSuccess() = runTest {
        val task = Task(id = "1", title = "Tarea 1")
        fakeRepository.givenTasks(task)

        val result = getTaskByIdUseCase("1")
        assertTrue(result is KronoResult.Success)
        assertEquals("Tarea 1", (result as KronoResult.Success).data.title)
    }

    @Test
    fun getTaskById_cuandoNoExiste_devuelveErrorTaskNotFound() = runTest {
        val result = getTaskByIdUseCase("no-id")
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.TaskNotFound, (result as KronoResult.Error).throwable)
    }

    @Test
    fun upsertTask_conTituloValido_guardaEnRepositorio() = runTest {
        val subtask = Subtask(id = "s1", taskId = "t1", title = "Paso 1")
        val task = Task(id = "t1", title = "Estudiar Kotlin", subtasks = listOf(subtask))

        val result = upsertTaskUseCase(task)
        assertTrue(result is KronoResult.Success)
        assertEquals(1, fakeRepository.upsertCalls)

        val stored = fakeRepository.getTaskById("t1")
        assertTrue(stored is KronoResult.Success)
        assertEquals(1, (stored as KronoResult.Success).data.subtasks.size)
    }

    @Test
    fun upsertTask_conTituloEnBlanco_devuelveValidationError() = runTest {
        val task = Task(id = "t1", title = "   ")

        val result = upsertTaskUseCase(task)
        assertTrue(result is KronoResult.Error)
        assertTrue((result as KronoResult.Error).throwable is TaskError.ValidationError)
        assertEquals(0, fakeRepository.upsertCalls)
    }

    @Test
    fun upsertTask_cuandoRepositorioFalla_devuelveError() = runTest {
        fakeRepository.failure = TaskError.StorageError("Fallo de red")
        val task = Task(id = "t1", title = "Tarea")

        val result = upsertTaskUseCase(task)
        assertTrue(result is KronoResult.Error)
        assertEquals(fakeRepository.failure, (result as KronoResult.Error).throwable)
    }

    @Test
    fun deleteTask_conIdValido_eliminaEnRepositorio() = runTest {
        val task = Task(id = "t1", title = "Tarea a eliminar")
        fakeRepository.givenTasks(task)

        val result = deleteTaskUseCase("t1")
        assertTrue(result is KronoResult.Success)
        assertEquals(1, fakeRepository.deleteCalls)

        val check = fakeRepository.getTaskById("t1")
        assertTrue(check is KronoResult.Error)
    }

    @Test
    fun deleteTask_conIdEnBlanco_devuelveValidationError() = runTest {
        val result = deleteTaskUseCase("   ")
        assertTrue(result is KronoResult.Error)
        assertTrue((result as KronoResult.Error).throwable is TaskError.ValidationError)
        assertEquals(0, fakeRepository.deleteCalls)
    }

    @Test
    fun updateTaskFields_conCamposDelFormulario_noAlteraLosDemasCampos() = runTest {
        val stored = Task(id = "t1", title = "Original", actualDuration = 30.minutes, isCompleted = true)
        fakeRepository.givenTasks(stored)
        val staleEdit = stored.copy(title = "Editada", actualDuration = Duration.ZERO, isCompleted = false)

        val result = updateTaskFieldsUseCase(staleEdit, TaskField.EDIT_FORM)

        assertEquals(KronoResult.Success(Unit), result)
        val updated = fakeRepository.storedTask("t1")
        assertEquals("Editada", updated?.title)
        assertEquals(30.minutes, updated?.actualDuration)
        assertTrue(updated?.isCompleted == true)
    }

    @Test
    fun updateTaskFields_conTituloEnBlanco_devuelveValidationErrorSinEscribir() = runTest {
        fakeRepository.givenTasks(Task(id = "t1", title = "Original"))

        val result = updateTaskFieldsUseCase(Task(id = "t1", title = "  "), setOf(TaskField.TITLE))

        assertTrue((result as KronoResult.Error).throwable is TaskError.ValidationError)
        assertEquals(0, fakeRepository.updateFieldsCalls)
    }

    @Test
    fun updateTaskFields_sinCampos_devuelveValidationErrorSinEscribir() = runTest {
        val result = updateTaskFieldsUseCase(Task(id = "t1", title = "Tarea"), emptySet())

        assertTrue((result as KronoResult.Error).throwable is TaskError.ValidationError)
        assertEquals(0, fakeRepository.updateFieldsCalls)
    }

    @Test
    fun updateTaskFields_sinTocarElTitulo_permiteTituloEnBlancoDelSnapshot() = runTest {
        fakeRepository.givenTasks(Task(id = "t1", title = "Original"))

        val result = updateTaskFieldsUseCase(Task(id = "t1", title = "", isCompleted = true), setOf(TaskField.IS_COMPLETED))

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals("Original", fakeRepository.storedTask("t1")?.title)
    }
}
