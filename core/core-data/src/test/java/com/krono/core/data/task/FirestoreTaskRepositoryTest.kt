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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.domain.task.TaskField
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas unitarias para [FirestoreTaskRepository] (F-02 CA-1, CA-2, CA-3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
            val list = (awaitItem() as KronoResult.Success).data
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
            val list = (awaitItem() as KronoResult.Success).data
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
            val task = (awaitItem() as KronoResult.Success).data
            assertEquals("Pagar matrícula", task?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTaskById_cuandoNoExiste_emiteNulo() = runTest {
        fakeAuthDataSource.user.value = testUser

        repository.observeTaskById("inexistente").test {
            assertEquals(KronoResult.Success(null), awaitItem())
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

    @Test
    fun upsertTask_sinConfirmacionDelServidor_devuelveSuccessYDejaLaTareaEnLaCacheLocal() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async { repository.upsertTask(Task(id = "t1", title = "Sin red")) }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
        assertEquals("Sin red", fakeTaskDataSource.storedTask(testUser.id, "t1")?.title)
    }

    @Test
    fun upsertTask_conConfirmacionRapida_noEsperaElTiempoLimite() = runTest {
        fakeAuthDataSource.user.value = testUser
        val confirmation = CompletableDeferred<Unit>()
        fakeTaskDataSource.serverConfirmation = confirmation

        val result = backgroundScope.async { repository.upsertTask(Task(id = "t1", title = "Con red")) }
        advanceTimeBy(FAST_CONFIRMATION)
        confirmation.complete(Unit)
        advanceTimeBy(FAST_CONFIRMATION)

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
    }

    @Test
    fun deleteTask_sinConfirmacionDelServidor_devuelveSuccess() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.givenTask(testUser.id, TaskDto(id = "t1", title = "Por borrar"))
        fakeTaskDataSource.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async { repository.deleteTask("t1") }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
        assertNull(fakeTaskDataSource.storedTask(testUser.id, "t1"))
    }

    @Test
    fun updateTaskFields_conSnapshotViejo_noPisaElTiempoRealNiLasSubtareas() = runTest {
        fakeAuthDataSource.user.value = testUser
        val subtask = SubtaskDto(id = "s1", taskId = "t1", title = "Paso").toMap()
        fakeTaskDataSource.givenTask(
            testUser.id,
            TaskDto(
                id = "t1",
                title = "Original",
                actualDurationMillis = 30.minutes.inWholeMilliseconds,
                isCompleted = true,
                subtasks = listOf(subtask),
            ),
        )
        val staleSnapshot = Task(id = "t1", title = "Editada", actualDuration = Duration.ZERO)

        val result = repository.updateTaskFields(staleSnapshot, TaskField.EDIT_FORM)

        assertEquals(KronoResult.Success(Unit), result)
        val stored = fakeTaskDataSource.storedTask(testUser.id, "t1")
        assertEquals("Editada", stored?.title)
        assertEquals(30.minutes.inWholeMilliseconds, stored?.actualDurationMillis)
        assertTrue(stored?.isCompleted == true)
        assertEquals(1, stored?.subtasks?.size)
        assertFalse(fakeTaskDataSource.lastUpdatedFields.containsKey(TaskDto.FIELD_ACTUAL_DURATION_MILLIS))
    }

    @Test
    fun updateTaskFields_sinConfirmacionDelServidor_devuelveSuccess() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.givenTask(testUser.id, TaskDto(id = "t1", title = "Original"))
        fakeTaskDataSource.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async {
            repository.updateTaskFields(Task(id = "t1", title = "Editada"), TaskField.EDIT_FORM)
        }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
    }

    @Test
    fun updateTaskFields_sinSesion_retornaErrorUnauthenticatedUser() = runTest {
        fakeAuthDataSource.user.value = null

        val result = repository.updateTaskFields(Task(id = "t1", title = "Editada"), TaskField.EDIT_FORM)

        assertEquals(KronoResult.Error(TaskError.UnauthenticatedUser), result)
    }

    @Test
    fun updateTaskFields_conErrorLocal_retornaError() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.failure = permissionDenied()

        val result = repository.updateTaskFields(Task(id = "t1", title = "Editada"), TaskField.EDIT_FORM)

        assertTrue((result as KronoResult.Error).throwable is TaskError.StorageError)
    }

    @Test
    fun observeTasks_cuandoElListenerFalla_emiteErrorEnVezDeListaVacia() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.listenerFailure = permissionDenied()

        repository.observeTasks().test {
            val error = (awaitItem() as KronoResult.Error).throwable
            assertTrue(error is TaskError.StorageError)
            awaitComplete()
        }
    }

    @Test
    fun observeTasks_alVolverARecolectarTrasUnFallo_reabreElListener() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.givenTask(testUser.id, TaskDto(id = "t1", title = "Recuperada"))
        fakeTaskDataSource.listenerFailure = permissionDenied()
        repository.observeTasks().test {
            awaitItem()
            awaitComplete()
        }

        fakeTaskDataSource.listenerFailure = null
        repository.observeTasks().test {
            val list = (awaitItem() as KronoResult.Success).data
            assertEquals("Recuperada", list.single().title)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, fakeTaskDataSource.tasksListenerCount)
    }

    @Test
    fun observeTaskById_cuandoElListenerFalla_emiteErrorEnVezDeNulo() = runTest {
        fakeAuthDataSource.user.value = testUser
        fakeTaskDataSource.givenTask(testUser.id, TaskDto(id = "t1", title = "Existe"))
        fakeTaskDataSource.listenerFailure = permissionDenied()

        repository.observeTaskById("t1").test {
            val error = (awaitItem() as KronoResult.Error).throwable
            assertTrue(error is TaskError.StorageError)
            awaitComplete()
        }
    }

    private fun permissionDenied() = FirebaseFirestoreException(
        "Permiso denegado",
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
    )

    private companion object {
        /** Retardo muy inferior a cualquier tiempo límite razonable de confirmación. */
        val FAST_CONFIRMATION = 1.seconds / 10

        /** Espera sin red muy superior a cualquier tiempo límite razonable de confirmación. */
        val OFFLINE_WAIT = 1.minutes
    }
}
