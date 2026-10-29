package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.Task
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.NoOpTaskRepository
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [ResetUserDataUseCase]: propagación de errores y limpieza del cronómetro (F-21 CA-1). */
class ResetUserDataUseCaseTest {

    private val log = mutableListOf<String>()
    private val remote = NoOpProfileRemoteDataSource(log)
    private val store = ProfileLocalStore().apply {
        bindUser(USER_ID, "user@krono.app")
        saveProfile(MODIFIED_NAME, MODIFIED_ROLE)
    }
    private val tasks = listOf(Task(id = "t1", title = "Uno"), Task(id = "t2", title = "Dos"))

    private fun useCase(taskRepo: NoOpTaskRepository) = ResetUserDataUseCase(store, taskRepo, remote)

    @Test
    fun errorAlBorrarUnaTareaDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log, failingTaskId = "t1")

        val result = useCase(taskRepo)()

        assertTrue(result is KronoResult.Error)
        assertTrue(NoOpProfileRemoteDataSource.CALL_RESET_USER_DATA !in log)
        assertEquals(MODIFIED_NAME, store.userName.value)
        assertEquals(MODIFIED_ROLE, store.userRole.value)
    }

    @Test
    fun errorAlObservarLasTareasDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        val taskRepo = object : NoOpTaskRepository() {
            override fun observeTasks(): Flow<List<Task>> = flow { throw IllegalStateException("sin red") }
        }

        val result = useCase(taskRepo)()

        assertTrue(result is KronoResult.Error)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun errorRemotoAlRestablecerElPerfilDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        remote.shouldFail = IllegalStateException("PERMISSION_DENIED")

        val result = useCase(RecordingTaskRepository(tasks, log))()

        assertTrue(result is KronoResult.Error)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun errorAlBorrarElCronometroActivoDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        remote.clearActiveTimerFailure = IllegalStateException("sin permiso")

        val result = useCase(RecordingTaskRepository(tasks, log))()

        assertTrue(result is KronoResult.Error)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun exitoCompletoBorraTareasYCronometroRestableceElPerfilYLimpiaElAlmacenLocal() = runTest {
        val result = useCase(RecordingTaskRepository(tasks, log))()

        assertTrue(result is KronoResult.Success)
        assertEquals(
            listOf(
                "deleteTask:t1",
                "deleteTask:t2",
                NoOpProfileRemoteDataSource.CALL_CLEAR_ACTIVE_TIMER,
                NoOpProfileRemoteDataSource.CALL_RESET_USER_DATA,
            ),
            log,
        )
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
    }

    private companion object {
        const val USER_ID = "user_abc"
        const val MODIFIED_NAME = "Nombre Modificado"
        const val MODIFIED_ROLE = "Rol Modificado"
    }
}
