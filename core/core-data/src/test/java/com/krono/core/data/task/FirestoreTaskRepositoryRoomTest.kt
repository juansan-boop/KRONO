package com.krono.core.data.task

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.data.local.room.FakeTaskDao
import com.krono.core.data.local.room.TaskEntity
import com.krono.core.data.local.room.toEntity
import com.krono.core.data.timer.FakeTimerRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.TaskPriority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias exhaustivas que validan la integración bidireccional entre [FirestoreTaskRepository]
 * y el almacenamiento local estructurado con [FakeTaskDao] (Room) (F-02, D-1).
 *
 * Cubre:
 * - Persistencia automática en Room con estado CONFIRMED ante emisiones remotas.
 * - Respaldo y recuperación offline cuando Firestore falla o no hay conexión.
 * - Almacenamiento local inmediato con estado PENDING_SYNC si la sincronización remota falla.
 * - Actualización parcial preservando campos intactos en Room.
 * - Eliminación física en Room y Firestore.
 * - Aislamiento multi-usuario en consultas reactivas y persistencia estructurada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskRepositoryRoomTest {

    private lateinit var fakeTimerDataSource: FakeTimerRemoteDataSource
    private lateinit var fakeTaskDataSource: FakeTaskRemoteDataSource
    private lateinit var fakeAuthDataSource: FakeAuthRemoteDataSource
    private lateinit var fakeTaskDao: FakeTaskDao
    private lateinit var repository: FirestoreTaskRepository

    private val testUser = User(id = "user-krono", email = "offline@krono.app")

    @Before
    fun setUp() {
        fakeTimerDataSource = FakeTimerRemoteDataSource()
        fakeTaskDataSource = FakeTaskRemoteDataSource(fakeTimerDataSource)
        fakeAuthDataSource = FakeAuthRemoteDataSource()
        fakeTaskDao = FakeTaskDao()

        fakeAuthDataSource.user.value = testUser
        repository = FirestoreTaskRepository(
            remoteDataSource = fakeTaskDataSource,
            authDataSource = fakeAuthDataSource,
            timerDataSource = fakeTimerDataSource,
            taskDao = fakeTaskDao,
        )
    }

    // =========================================================================
    // Observación reactiva y recuperación offline (observeTasks / observeTaskById)
    // =========================================================================

    /**
     * Verifica que cuando Firestore emite una lista remota, se persista automáticamente
     * en Room con estado STATUS_CONFIRMED.
     */
    @Test
    fun observeTasks_cuandoRemotoEmite_persisteAutomaticamenteEnRoomComoConfirmed() = runTest {
        val taskDto = TaskDto(
            id = "t-room-1",
            title = "Tarea sincronizada en Room",
            category = TaskCategory.TRABAJO.name,
            priority = TaskPriority.ALTA.name,
        )
        fakeTaskDataSource.givenTask(testUser.id, taskDto)

        repository.observeTasks().test {
            val list = (awaitItem() as KronoResult.Success).data
            assertEquals(1, list.size)
            assertEquals("Tarea sincronizada en Room", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        val cached = fakeTaskDao.getTaskById(testUser.id, "t-room-1")
        assertNotNull(cached)
        assertEquals("Tarea sincronizada en Room", cached?.title)
        assertEquals(TaskEntity.STATUS_CONFIRMED, cached?.syncStatus)
    }

    /**
     * Verifica que si el listener remoto de Firestore falla pero Room posee tareas en caché,
     * el repositorio emita exitosamente las tareas desde Room.
     */
    @Test
    fun observeTasks_cuandoRemotoFallaPeroRoomTieneCache_emiteTareasDeLaCache() = runTest {
        val cachedTask = Task(
            id = "t-offline",
            title = "Tarea en caché local SQLite",
            category = TaskCategory.ESTUDIO,
            priority = TaskPriority.MEDIA,
        )
        fakeTaskDao.upsertTask(cachedTask.toEntity(testUser.id, TaskEntity.STATUS_CONFIRMED))

        fakeTaskDataSource.listenerFailure = FirebaseFirestoreException(
            "Sin red",
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )

        repository.observeTasks().test {
            val result = awaitItem()
            assertTrue(result is KronoResult.Success)
            val list = (result as KronoResult.Success).data
            assertEquals(1, list.size)
            assertEquals("Tarea en caché local SQLite", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que si el listener remoto falla y Room no tiene tareas cacheadas,
     * se propague el error correspondiente.
     */
    @Test
    fun observeTasks_cuandoRemotoFallaYRoomVacio_emiteError() = runTest {
        fakeTaskDataSource.listenerFailure = FirebaseFirestoreException(
            "Sin red y sin datos",
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )

        repository.observeTasks().test {
            val result = awaitItem()
            assertTrue(result is KronoResult.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que observeTasks filtre estrictamente por el usuario activo en la sesión.
     */
    @Test
    fun observeTasks_aislamientoMultiUsuario_soloEmiteTareasDelUsuarioAutenticado() = runTest {
        val otherUser = User(id = "user-other", email = "other@krono.app")

        fakeTaskDataSource.givenTask(testUser.id, TaskDto(id = "t-u1", title = "Tarea Usuario 1"))
        fakeTaskDataSource.givenTask(otherUser.id, TaskDto(id = "t-u2", title = "Tarea Usuario 2"))

        repository.observeTasks().test {
            val list = (awaitItem() as KronoResult.Success).data
            assertEquals(1, list.size)
            assertEquals("Tarea Usuario 1", list[0].title)

            // Cambiamos de usuario autenticado
            fakeAuthDataSource.user.value = otherUser
            val listOther = (awaitItem() as KronoResult.Success).data
            assertEquals(1, listOther.size)
            assertEquals("Tarea Usuario 2", listOther[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que observeTaskById recupere la tarea de Room si el stream de Firestore falla.
     */
    @Test
    fun observeTaskById_cuandoRemotoFallaPeroRoomTieneCache_emiteTareaDeRoom() = runTest {
        val cachedTask = Task(id = "t-puntual", title = "Puntual en Room")
        fakeTaskDao.upsertTask(cachedTask.toEntity(testUser.id, TaskEntity.STATUS_CONFIRMED))

        fakeTaskDataSource.listenerFailure = FirebaseFirestoreException(
            "Desconectado",
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )

        repository.observeTaskById("t-puntual").test {
            val result = awaitItem()
            assertTrue(result is KronoResult.Success)
            val task = (result as KronoResult.Success).data
            assertEquals("Puntual en Room", task?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que observeTaskById propague error si Firestore falla y la tarea no está en Room.
     */
    @Test
    fun observeTaskById_cuandoRemotoFallaYRoomNoTieneTarea_emiteError() = runTest {
        fakeTaskDataSource.listenerFailure = FirebaseFirestoreException(
            "Sin conexión",
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )

        repository.observeTaskById("t-inexistente").test {
            val result = awaitItem()
            assertTrue(result is KronoResult.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // Consulta directa (getTaskById)
    // =========================================================================

    /**
     * Verifica que getTaskById recupere la tarea desde Room cuando Firestore lanza error de red.
     */
    @Test
    fun getTaskById_cuandoRemotoFallaPeroRoomTieneCache_recuperaDeRoom() = runTest {
        val cachedTask = Task(id = "t-offline-get", title = "Recuperada sin conexión")
        fakeTaskDao.upsertTask(cachedTask.toEntity(testUser.id, TaskEntity.STATUS_CONFIRMED))

        fakeTaskDataSource.failure = IOException("Sin conexión a Firestore")

        val result = repository.getTaskById("t-offline-get")
        assertTrue(result is KronoResult.Success)
        assertEquals("Recuperada sin conexión", (result as KronoResult.Success).data.title)
    }

    /**
     * Verifica que getTaskById actualice Room con estado CONFIRMED cuando la consulta remota es exitosa.
     */
    @Test
    fun getTaskById_cuandoRemotoRetornaTarea_actualizaRoomConStatusConfirmed() = runTest {
        val remoteDto = TaskDto(id = "t-remota", title = "Remota Exitosa")
        fakeTaskDataSource.givenTask(testUser.id, remoteDto)

        val result = repository.getTaskById("t-remota")
        assertTrue(result is KronoResult.Success)
        assertEquals("Remota Exitosa", (result as KronoResult.Success).data.title)

        val cached = fakeTaskDao.getTaskById(testUser.id, "t-remota")
        assertNotNull(cached)
        assertEquals(TaskEntity.STATUS_CONFIRMED, cached?.syncStatus)
    }

    /**
     * Verifica que getTaskById devuelva error si Firestore falla y la tarea no existe en Room.
     */
    @Test
    fun getTaskById_cuandoRemotoFallaYNoExisteEnRoom_retornaError() = runTest {
        fakeTaskDataSource.failure = IOException("Error de red total")

        val result = repository.getTaskById("t-desconocida")
        assertTrue(result is KronoResult.Error)
    }

    /**
     * Verifica que getTaskById devuelva TaskNotFound cuando remoto devuelve null y Room no la tiene.
     */
    @Test
    fun getTaskById_cuandoRemotoRetornaNullYNoExisteEnRoom_retornaTaskNotFoundError() = runTest {
        val result = repository.getTaskById("t-inexistente-total")
        assertTrue(result is KronoResult.Error)
        assertEquals(TaskError.TaskNotFound, (result as KronoResult.Error).throwable)
    }

    // =========================================================================
    // Creación, Actualización y Borrado (upsertTask, updateTaskFields, deleteTask)
    // =========================================================================

    /**
     * Verifica que upsertTask guarde la tarea en Room inmediatamente con estado CONFIRMED
     * al sincronizar con éxito.
     */
    @Test
    fun upsertTask_guardaEnRoomConRespaldoInmediato() = runTest {
        val task = Task(
            id = "t-nueva",
            title = "Guardar y sincronizar",
            subtasks = listOf(Subtask(id = "s-1", taskId = "t-nueva", title = "Paso 1")),
        )

        val result = repository.upsertTask(task)
        assertTrue(result is KronoResult.Success)

        val cached = fakeTaskDao.getTaskById(testUser.id, "t-nueva")
        assertNotNull(cached)
        assertEquals("Guardar y sincronizar", cached?.title)
        assertEquals(TaskEntity.STATUS_CONFIRMED, cached?.syncStatus)
        assertTrue(cached!!.subtasksJson.contains("Paso 1"))
    }

    /**
     * Verifica que ante falla remota en upsertTask, la tarea quede registrada en Room
     * con estado PENDING_SYNC para sincronización diferida posterior.
     */
    @Test
    fun upsertTask_cuandoRemotoFalla_conservaRegistroEnRoomComoPendingSync() = runTest {
        fakeTaskDataSource.failure = IOException("Error de red al guardar en Firestore")

        val task = Task(
            id = "t-offline-sync",
            title = "Tarea creada sin red",
            category = TaskCategory.PERSONAL,
            priority = TaskPriority.ALTA,
        )

        val result = repository.upsertTask(task)
        assertTrue(result is KronoResult.Error)

        // Verificamos que la tarea persiste en Room pero con estado PENDING_SYNC
        val cached = fakeTaskDao.getTaskById(testUser.id, "t-offline-sync")
        assertNotNull(cached)
        assertEquals("Tarea creada sin red", cached?.title)
        assertEquals(TaskEntity.STATUS_PENDING_SYNC, cached?.syncStatus)

        val pendingList = fakeTaskDao.getPendingSyncTasks(testUser.id)
        assertEquals(1, pendingList.size)
        assertEquals("t-offline-sync", pendingList[0].id)
    }

    /**
     * Verifica que updateTaskFields actualice selectivamente los campos en Room
     * preservando intactos todos los demás datos existentes.
     */
    @Test
    fun updateTaskFields_actualizaRegistroEnRoomYPreservaCamposNoModificados() = runTest {
        val fullTask = Task(
            id = "t-update-fields",
            title = "Título Inicial",
            category = TaskCategory.ESTUDIO,
            priority = TaskPriority.BAJA,
            dueDate = LocalDateTime(2026, 12, 10, 10, 0),
            estimatedDuration = 3.hours,
            actualDuration = 30.minutes,
            isCompleted = false,
            note = "Nota original importante",
            subtasks = listOf(Subtask(id = "s-initial", taskId = "t-update-fields", title = "Subtarea")),
        )
        repository.upsertTask(fullTask)

        // Modificamos únicamente el título y el estado de completado
        val modified = fullTask.copy(
            title = "Título Nuevo",
            isCompleted = true,
        )
        val updateResult = repository.updateTaskFields(modified, setOf(TaskField.TITLE, TaskField.IS_COMPLETED))
        assertTrue(updateResult is KronoResult.Success)

        val cached = fakeTaskDao.getTaskById(testUser.id, "t-update-fields")
        assertNotNull(cached)
        assertEquals("Título Nuevo", cached?.title)
        assertTrue(cached!!.isCompleted)
        // Verificamos que los campos no tocados permanezcan idénticos en Room
        assertEquals("ESTUDIO", cached.category)
        assertEquals("BAJA", cached.priority)
        assertEquals("2026-12-10T10:00", cached.dueDateIso)
        assertEquals(3.hours.inWholeMilliseconds, cached.estimatedDurationMillis)
        assertEquals("Nota original importante", cached.note)
        assertTrue(cached.subtasksJson.contains("s-initial"))
        assertEquals(TaskEntity.STATUS_CONFIRMED, cached.syncStatus)
    }

    /**
     * Verifica que deleteTask elimine la tarea tanto de Firestore como de la base local Room.
     */
    @Test
    fun deleteTask_eliminaLaTareaDeRoom() = runTest {
        val task = Task(id = "t-borrar", title = "Por eliminar")
        repository.upsertTask(task)
        assertNotNull(fakeTaskDao.getTaskById(testUser.id, "t-borrar"))

        val deleteResult = repository.deleteTask("t-borrar")
        assertTrue(deleteResult is KronoResult.Success)

        assertNull(fakeTaskDao.getTaskById(testUser.id, "t-borrar"))
    }
}
