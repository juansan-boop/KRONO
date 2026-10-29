package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.Task
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [DeleteAccountUseCase]: orden seguro de borrado y propagación de errores (F-21 CA-2). */
class DeleteAccountUseCaseTest {

    private val log = mutableListOf<String>()
    private val remote = NoOpProfileRemoteDataSource(log)
    private val store = ProfileLocalStore().apply {
        bindUser(USER_ID, "user@krono.app")
        saveProfile(MODIFIED_NAME, MODIFIED_ROLE)
    }
    private val tasks = listOf(Task(id = "t1", title = "Uno"), Task(id = "t2", title = "Dos"))

    private fun useCase(taskRepo: RecordingTaskRepository) = DeleteAccountUseCase(store, taskRepo, remote)

    @Test
    fun reautenticacionFallidaNoBorraNingunaTareaNiLaCuenta() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log)
        remote.reauthenticateFailure = IllegalStateException("contraseña incorrecta")

        val result = useCase(taskRepo)(PASSWORD)

        assertTrue(result is KronoResult.Error)
        assertEquals("contraseña incorrecta", (result as KronoResult.Error).throwable.message)
        assertEquals(listOf(NoOpProfileRemoteDataSource.CALL_REAUTHENTICATE), log)
        assertEquals(2, taskRepo.tasksFlow.value.size)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun errorAlBorrarUnaTareaDevuelveErrorSinEliminarLaCuentaNiLimpiarElAlmacenLocal() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log, failingTaskId = "t2")

        val result = useCase(taskRepo)(PASSWORD)

        assertTrue(result is KronoResult.Error)
        assertTrue(NoOpProfileRemoteDataSource.CALL_DELETE_ACCOUNT !in log)
        assertTrue(NoOpProfileRemoteDataSource.CALL_CLEAR_ACTIVE_TIMER !in log)
        assertEquals(MODIFIED_NAME, store.userName.value)
        assertEquals(MODIFIED_ROLE, store.userRole.value)
    }

    @Test
    fun exitoCompletoReautenticaLuegoBorraTareasYCronometroYPorUltimoEliminaLaCuenta() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log)

        val result = useCase(taskRepo)(PASSWORD)

        assertTrue(result is KronoResult.Success)
        assertEquals(
            listOf(
                NoOpProfileRemoteDataSource.CALL_REAUTHENTICATE,
                "deleteTask:t1",
                "deleteTask:t2",
                NoOpProfileRemoteDataSource.CALL_CLEAR_ACTIVE_TIMER,
                NoOpProfileRemoteDataSource.CALL_DELETE_ACCOUNT,
            ),
            log,
        )
        assertEquals(PASSWORD, remote.lastDeletePassword)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
    }

    @Test
    fun errorAlBorrarElCronometroActivoDevuelveErrorSinEliminarLaCuenta() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log)
        remote.clearActiveTimerFailure = IllegalStateException("sin permiso")

        val result = useCase(taskRepo)(PASSWORD)

        assertTrue(result is KronoResult.Error)
        assertTrue(NoOpProfileRemoteDataSource.CALL_DELETE_ACCOUNT !in log)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun errorAlEliminarLaCuentaDevuelveErrorYConservaElAlmacenLocal() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log)
        remote.shouldFail = IllegalStateException("fallo de Auth")

        val result = useCase(taskRepo)(PASSWORD)

        assertTrue(result is KronoResult.Error)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun contrasenaEnBlancoDevuelveErrorSinTocarNada() = runTest {
        val taskRepo = RecordingTaskRepository(tasks, log)

        val result = useCase(taskRepo)("  ")

        assertTrue(result is KronoResult.Error)
        assertTrue(log.isEmpty())
    }

    private companion object {
        const val USER_ID = "user_abc"
        const val PASSWORD = "Password123*"
        const val MODIFIED_NAME = "Nombre Modificado"
        const val MODIFIED_ROLE = "Rol Modificado"
    }
}
