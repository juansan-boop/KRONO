package com.krono.core.data.local.room

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [FakeTaskDao] asegurando que el doble de prueba en memoria
 * implementa fielmente los contratos de consulta y persistencia de [TaskDao] (Room).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeTaskDaoTest {

    private lateinit var dao: FakeTaskDao

    private fun createEntity(
        id: String,
        userId: String = "user-1",
        title: String = "Tarea $id",
        isDeleted: Boolean = false,
        syncStatus: String = TaskEntity.STATUS_CONFIRMED,
    ) = TaskEntity(
        id = id,
        userId = userId,
        title = title,
        category = "TRABAJO",
        priority = "ALTA",
        dueDateIso = null,
        estimatedDurationMillis = null,
        actualDurationMillis = 0L,
        isCompleted = false,
        note = null,
        subtasksJson = "[]",
        isDeleted = isDeleted,
        syncStatus = syncStatus,
    )

    @Before
    fun setUp() {
        dao = FakeTaskDao()
    }

    /**
     * Verifica que upsertTask almacene la entidad y getTaskById la recupere fielmente.
     */
    @Test
    fun upsertTask_y_getTaskById_almacenaYRecuperaEntidadCorrectamente() = runTest {
        val task = createEntity(id = "t-1", userId = "user-1", title = "Primera Tarea")
        dao.upsertTask(task)

        val retrieved = dao.getTaskById("user-1", "t-1")
        assertNotNull(retrieved)
        assertEquals("t-1", retrieved?.id)
        assertEquals("Primera Tarea", retrieved?.title)

        // Verificamos que consultar con otro usuario retorne null (aislamiento por userId)
        val otherUser = dao.getTaskById("user-2", "t-1")
        assertNull(otherUser)
    }

    /**
     * Verifica que upsertTasks almacene múltiples entidades y getTasks devuelva solo las activas del usuario.
     */
    @Test
    fun upsertTasks_y_getTasks_recuperaSoloTareasActivasDelUsuario() = runTest {
        val tasks = listOf(
            createEntity(id = "t-1", userId = "user-1", isDeleted = false),
            createEntity(id = "t-2", userId = "user-1", isDeleted = true),
            createEntity(id = "t-3", userId = "user-2", isDeleted = false),
        )
        dao.upsertTasks(tasks)

        val user1Tasks = dao.getTasks("user-1")
        assertEquals(1, user1Tasks.size)
        assertEquals("t-1", user1Tasks[0].id)

        val user2Tasks = dao.getTasks("user-2")
        assertEquals(1, user2Tasks.size)
        assertEquals("t-3", user2Tasks[0].id)
    }

    /**
     * Verifica que observeTasks emita reactivamente los cambios ante inserciones o modificaciones.
     */
    @Test
    fun observeTasks_emiteCambiosReactivamente() = runTest {
        dao.observeTasks("user-1").test {
            // Emisión inicial vacía
            assertEquals(0, awaitItem().size)

            // Insertamos una tarea activa
            dao.upsertTask(createEntity(id = "t-obs-1", userId = "user-1"))
            val item1 = awaitItem()
            assertEquals(1, item1.size)
            assertEquals("t-obs-1", item1[0].id)

            // Insertamos una tarea eliminada lógicamente (no debe aparecer en observeTasks)
            dao.upsertTask(createEntity(id = "t-obs-2", userId = "user-1", isDeleted = true))
            // No emite nueva lista porque la lista filtrada no cambió (o emite la misma si no hay distinct, pero el tamaño sigue 1)
            // Actualizamos la tarea 1
            dao.upsertTask(createEntity(id = "t-obs-1", userId = "user-1", title = "Título Actualizado"))
            val item2 = awaitItem()
            assertEquals(1, item2.size)
            assertEquals("Título Actualizado", item2[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que observeTaskById emita los cambios de una tarea puntual o null si no existe.
     */
    @Test
    fun observeTaskById_emiteCambioPuntual() = runTest {
        dao.observeTaskById("user-1", "t-target").test {
            assertNull(awaitItem())

            dao.upsertTask(createEntity(id = "t-target", userId = "user-1", title = "Objetivo"))
            val item = awaitItem()
            assertNotNull(item)
            assertEquals("Objetivo", item?.title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica que getPendingSyncTasks devuelva únicamente las tareas con estado PENDING_SYNC.
     */
    @Test
    fun getPendingSyncTasks_filtraSoloTareasEnEstadoPendingSync() = runTest {
        dao.upsertTasks(
            listOf(
                createEntity(id = "t-conf", userId = "user-1", syncStatus = TaskEntity.STATUS_CONFIRMED),
                createEntity(id = "t-pend-1", userId = "user-1", syncStatus = TaskEntity.STATUS_PENDING_SYNC),
                createEntity(id = "t-pend-2", userId = "user-1", syncStatus = TaskEntity.STATUS_PENDING_SYNC),
                createEntity(id = "t-failed", userId = "user-1", syncStatus = TaskEntity.STATUS_FAILED),
                createEntity(id = "t-other-pend", userId = "user-2", syncStatus = TaskEntity.STATUS_PENDING_SYNC),
            )
        )

        val pendingUser1 = dao.getPendingSyncTasks("user-1")
        assertEquals(2, pendingUser1.size)
        val pendingIds = pendingUser1.map { it.id }.toSet()
        assertTrue(pendingIds.contains("t-pend-1"))
        assertTrue(pendingIds.contains("t-pend-2"))
    }

    /**
     * Verifica que clearTasksForUser purgue exclusivamente las tareas del usuario indicado.
     */
    @Test
    fun clearTasksForUser_eliminaSoloTareasDelUsuarioEspecificado() = runTest {
        dao.upsertTasks(
            listOf(
                createEntity(id = "t-u1-a", userId = "user-1"),
                createEntity(id = "t-u1-b", userId = "user-1"),
                createEntity(id = "t-u2-a", userId = "user-2"),
            )
        )

        dao.clearTasksForUser("user-1")

        assertTrue(dao.getTasks("user-1").isEmpty())
        assertEquals(1, dao.getTasks("user-2").size)
    }

    /**
     * Verifica que deleteTask elimine físicamente una tarea concreta del usuario en caché.
     */
    @Test
    fun deleteTask_eliminaTareaEspecificaDelUsuario() = runTest {
        dao.upsertTask(createEntity(id = "t-del", userId = "user-1"))
        assertNotNull(dao.getTaskById("user-1", "t-del"))

        // Intento de borrado por usuario no propietario no hace nada
        dao.deleteTask("user-2", "t-del")
        assertNotNull(dao.getTaskById("user-1", "t-del"))

        // Borrado por el usuario propietario
        dao.deleteTask("user-1", "t-del")
        assertNull(dao.getTaskById("user-1", "t-del"))
    }
}
