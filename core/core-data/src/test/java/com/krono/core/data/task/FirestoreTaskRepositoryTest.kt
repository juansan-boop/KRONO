package com.krono.core.data.task

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Pruebas unitarias para [FirestoreTaskRepository] (F-02 CA-1, CA-2, CA-3).
 */
class FirestoreTaskRepositoryTest {

    private lateinit var fakeTaskDataSource: FakeTaskRemoteDataSource
    private lateinit var fakeAuthDataSource: FakeAuthRemoteDataSource
    private lateinit var repository: FirestoreTaskRepository

    private val testUser = User(id = "user-123", email = "estudiante@krono.app")

    @Before
    fun setUp() {
        fakeTaskDataSource = FakeTaskRemoteDataSource()
        fakeAuthDataSource = FakeAuthRemoteDataSource()
        repository = FirestoreTaskRepository(fakeTaskDataSource, fakeAuthDataSource)
    }

    @Test
    fun observeTasks_cuandoUsuarioAutenticado_emiteListaDeTareas() = runTest {
        fakeAuthDataSource.user.value = testUser
        val taskDto = TaskDto(
            id = "t1",
            title = "Estudiar Cálculo",
            category = TaskCategory.ESTUDIO.name,
            priority = TaskPriority.ALTA.name,
        )
        fakeTaskDataSource.givenTask(testUser.id, taskDto)

        repository.observeTasks().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Estudiar Cálculo", list[0].title)
            assertEquals(TaskCategory.ESTUDIO, list[0].category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTasks_cuandoUsuarioNoAutenticado_emiteListaVacia() = runTest {
        fakeAuthDataSource.user.value = null

        repository.observeTasks().test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTaskById_cuandoTareaExiste_emiteTarea() = runTest {
        fakeAuthDataSource.user.value = testUser
        val taskDto = TaskDto(id = "t1", title = "Pagar matrícula")
        fakeTaskDataSource.givenTask(testUser.id, taskDto)

        repository.observeTaskById("t1").test {
            val task = awaitItem()
            assertEquals("Pagar matrícula", task?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTaskById_cuandoNoExiste_emiteNulo() = runTest {
        fakeAuthDataSource.user.value = testUser

        repository.observeTaskById("inexistente").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTaskById_conSesionActiva_retornaSuccess() = runTest {
        fakeAuthDataSource.user.value = testUser
        val taskDto = TaskDto(id = "t1", title = "Tarea encontrada")
        fakeTaskDataSource.givenTask(testUser.id, taskDto)

        val result = repository.getTaskById("t1")
        assertTrue(result is KronoResult.Success)
        assertEquals("Tarea encontrada", (result as KronoResult.Success).data.title)
    }

    @Test
    fun getTaskById_tareaInexistente_retornaErrorTaskNotFound() = runTest {
        fakeAuthDataSource.user.value = testUser

        val result = repository.getTaskById("no-existe")
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.TaskNotFound, (result as KronoResult.Error).throwable)
    }

    @Test
    fun getTaskById_sinSesion_retornaErrorUnauthenticatedUser() = runTest {
        fakeAuthDataSource.user.value = null

        val result = repository.getTaskById("t1")
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.UnauthenticatedUser, (result as KronoResult.Error).throwable)
    }

    @Test
    fun upsertTask_conSesionActiva_persisteYRetornaSuccess() = runTest {
        fakeAuthDataSource.user.value = testUser
        val subtask = Subtask(id = "s1", taskId = "t1", title = "Subtarea 1")
        val task = Task(
            id = "t1",
            title = "Nueva tarea",
            category = TaskCategory.TRABAJO,
            subtasks = listOf(subtask),
        )

        val result = repository.upsertTask(task)
        assertTrue(result is KronoResult.Success)

        val stored = fakeTaskDataSource.getTask(testUser.id, "t1")
        assertEquals("Nueva tarea", stored?.title)
        assertEquals(1, stored?.subtasks?.size)
    }

    @Test
    fun upsertTask_sinSesion_retornaErrorUnauthenticatedUser() = runTest {
        fakeAuthDataSource.user.value = null
        val task = Task(id = "t1", title = "Tarea sin sesión")

        val result = repository.upsertTask(task)
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.UnauthenticatedUser, (result as KronoResult.Error).throwable)
    }

    @Test
    fun deleteTask_conSesionActiva_eliminaYRetornaSuccess() = runTest {
        fakeAuthDataSource.user.value = testUser
        val taskDto = TaskDto(id = "t1", title = "Por borrar")
        fakeTaskDataSource.givenTask(testUser.id, taskDto)

        val result = repository.deleteTask("t1")
        assertTrue(result is KronoResult.Success)

        assertNull(fakeTaskDataSource.getTask(testUser.id, "t1"))
    }

    @Test
    fun deleteTask_sinSesion_retornaErrorUnauthenticatedUser() = runTest {
        fakeAuthDataSource.user.value = null

        val result = repository.deleteTask("t1")
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.UnauthenticatedUser, (result as KronoResult.Error).throwable)
    }

    @Test
    fun cuandoDataSourceLanzaExcepcion_retornaKronoResultError() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.failure = IOException("Error simulado de red")

        val result = repository.getTaskById("t1")
        assertTrue(result is KronoResult.Error)
        val error = (result as KronoResult.Error).throwable
        assertTrue(error is TaskError.StorageError)
    }
}
