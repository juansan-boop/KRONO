package com.krono.feature.profile.security

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.Task
import com.krono.feature.profile.MainDispatcherRule
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.NoOpTaskRepository
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.domain.DeleteAccountUseCase

import com.krono.feature.profile.domain.ResetUserDataUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Doble de prueba para [ResetUserDataUseCase] que permite compuertas de suspensión ([gate]),
 * simular fallos y registrar invocaciones.
 */
private class TestResetUserDataUseCase : ResetUserDataUseCase() {
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var callCount: Int = 0
        private set

    override suspend operator fun invoke(): KronoResult<Unit> {
        callCount++
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return super.invoke()
    }
}

/**
 * Doble de prueba para [DeleteAccountUseCase] que permite compuertas de suspensión ([gate]),
 * verificar la contraseña de reautenticación y registrar invocaciones.
 */
private class TestDeleteAccountUseCase : DeleteAccountUseCase() {
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var callCount: Int = 0
        private set
    var lastPassword: String? = null
        private set

    override suspend operator fun invoke(password: String): KronoResult<Unit> {
        callCount++
        lastPassword = password
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return super.invoke(password)
    }
}

/**
 * Pruebas unitarias para [SecurityZoneViewModel] (F-21 CA-1, CA-2: Zona de Seguridad).
 *
 * Valida de forma exhaustiva:
 * - El estado inicial y la selección de acciones (Paso 1).
 * - El flujo en 2 pasos de "Restablecer configuración y datos" (CA-1): apertura, descarte, éxito y error.
 * - El flujo en 2 pasos de "Eliminar cuenta y perfil" (CA-2): apertura, validación de contraseña de reautenticación, éxito y error.
 * - La protección concurrente contra doble clic y bloqueo de descarte mientras hay operaciones activas.
 * - La navegación hacia atrás mediante [SecurityZoneEvent.NavigateBack].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityZoneViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val resetUserDataUseCase = TestResetUserDataUseCase()
    private val deleteAccountUseCase = TestDeleteAccountUseCase()

    private fun createViewModel(): SecurityZoneViewModel =
        SecurityZoneViewModel(
            resetUserData = resetUserDataUseCase,
            deleteAccount = deleteAccountUseCase,
        )

    @Test
    fun elEstadoInicialReflejaValoresPorDefecto() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertEquals(SecurityActionType.RESET_DATA, state.selectedAction)
        assertFalse(state.showResetConfirmationDialog)
        assertFalse(state.showDeleteAccountDialog)
        assertFalse(state.isResettingData)
        assertFalse(state.isDeletingAccount)
        assertEquals("", state.reauthPassword)
        assertNull(state.reauthPasswordError)
        assertFalse(state.isPasswordVisible)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertFalse(state.isOperationInProgress)
    }

    @Test
    fun alternarAccionesActualizaElTipoSeleccionado() {
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        assertEquals(SecurityActionType.DELETE_ACCOUNT, vm.uiState.value.selectedAction)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        assertEquals(SecurityActionType.RESET_DATA, vm.uiState.value.selectedAction)
    }

    @Test
    fun paso2RestablecerDatosAbreDialogoYDescartarLoCierra() {
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        assertTrue(vm.uiState.value.showResetConfirmationDialog)

        vm.onDismissResetDialog()
        assertFalse(vm.uiState.value.showResetConfirmationDialog)
        assertEquals(0, resetUserDataUseCase.callCount)
    }

    @Test
    fun confirmarRestablecerDatosExitosoEmiteDataResetSuccess() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.RESET_DATA)
            vm.onContinueToConfirmation()
            vm.onConfirmResetData()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isResettingData)
            assertFalse(state.showResetConfirmationDialog)
            assertNotNull(state.successMessage)
            assertEquals(1, resetUserDataUseCase.callCount)

            assertEquals(SecurityZoneEvent.DataResetSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fallaEnRestablecerDatosExponeMensajeDeError() = runTest {
        resetUserDataUseCase.failure = RuntimeException("Error en Firestore al purgar")
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.RESET_DATA)
            vm.onContinueToConfirmation()
            vm.onConfirmResetData()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isResettingData)
            assertEquals("Error en Firestore al purgar", state.errorMessage)

            expectNoEvents()
        }
    }

    @Test
    fun paso2EliminarCuentaSinContrasenaValidaMuestraErrorYNoInvocaCasoDeUso() {
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        assertTrue(vm.uiState.value.showDeleteAccountDialog)

        vm.onConfirmDeleteAccount()

        val state = vm.uiState.value
        assertNotNull(state.reauthPasswordError)
        assertEquals("Ingresa tu contraseña actual para confirmar", state.reauthPasswordError)
        assertFalse(state.isDeletingAccount)
        assertEquals(0, deleteAccountUseCase.callCount)
    }

    @Test
    fun escribirContrasenaValidaLimpiaErrorYAlternaVisibilidad() {
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onConfirmDeleteAccount()
        assertNotNull(vm.uiState.value.reauthPasswordError)

        vm.onReauthPasswordChange("MiClaveSegura123!")
        assertNull(vm.uiState.value.reauthPasswordError)
        assertEquals("MiClaveSegura123!", vm.uiState.value.reauthPassword)

        assertFalse(vm.uiState.value.isPasswordVisible)
        vm.onTogglePasswordVisibility()
        assertTrue(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun confirmarEliminarCuentaConReautenticacionExitosaEmiteAccountDeleted() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
            vm.onContinueToConfirmation()
            vm.onReauthPasswordChange("Password123*")
            vm.onConfirmDeleteAccount()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isDeletingAccount)
            assertFalse(state.showDeleteAccountDialog)
            assertEquals("Password123*", deleteAccountUseCase.lastPassword)
            assertEquals(1, deleteAccountUseCase.callCount)

            assertEquals(SecurityZoneEvent.AccountDeleted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fallaEnEliminarCuentaExponeMensajeDeError() = runTest {
        deleteAccountUseCase.failure = RuntimeException("Credenciales de reautenticación inválidas")
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
            vm.onContinueToConfirmation()
            vm.onReauthPasswordChange("Password123*")
            vm.onConfirmDeleteAccount()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isDeletingAccount)
            assertEquals("Credenciales de reautenticación inválidas", state.errorMessage)

            expectNoEvents()
        }
    }

    @Test
    fun operacionEnCursoBloqueaDobleEnvioYDescarteDeModal() = runTest {
        val gate = CompletableDeferred<Unit>()
        resetUserDataUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.RESET_DATA)
            vm.onContinueToConfirmation()
            vm.onConfirmResetData()

            // Estado bloqueado por suspensión
            assertTrue(vm.uiState.value.isResettingData)
            assertTrue(vm.uiState.value.isOperationInProgress)

            // Intento concurrente de volver a confirmar (doble clic)
            vm.onConfirmResetData()

            // Intento de descartar el diálogo durante la ejecución (debe ignorarse)
            vm.onDismissResetDialog()
            assertTrue(vm.uiState.value.showResetConfirmationDialog)

            // Liberamos compuerta
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(SecurityZoneEvent.DataResetSuccess, awaitItem())
            expectNoEvents()
        }

        assertEquals(1, resetUserDataUseCase.callCount)
        assertFalse(vm.uiState.value.isResettingData)
    }

    @Test
    fun botonVolverEmiteNavigateBack() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onBackClick()
            advanceUntilIdle()

            assertEquals(SecurityZoneEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restablecerDatosRestauraValoresPorDefectoEnProfileLocalStore() = runTest {
        val store = ProfileLocalStore()
        store.saveProfile("Usuario Modificado", "Rol Modificado")
        store.setDynamicBreaksEnabled(false)

        val resetUseCase = ResetUserDataUseCase(store)
        val deleteUseCase = DeleteAccountUseCase(store)
        val vm = SecurityZoneViewModel(resetUseCase, deleteUseCase)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals(ProfileLocalStore.DEFAULT_NAME, store.userName.value)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
        assertTrue(store.dynamicBreaksEnabled.value)
    }

    @Test
    fun restablecerDatosEliminaTareasEnTaskRepositoryYSincronizaConRemoteDataSource() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_abc", "user@krono.app")
        val initialTasks = listOf(
            Task(id = "task-1", title = "Tarea 1"),
            Task(id = "task-2", title = "Tarea 2"),
        )
        val taskRepo = NoOpTaskRepository(initialTasks)
        val remoteDataSource = NoOpProfileRemoteDataSource()

        val resetUseCase = ResetUserDataUseCase(
            localStore = store,
            taskRepository = taskRepo,
            remoteDataSource = remoteDataSource,
        )
        val deleteUseCase = DeleteAccountUseCase(
            localStore = store,
            taskRepository = taskRepo,
            remoteDataSource = remoteDataSource,
        )
        val vm = SecurityZoneViewModel(resetUseCase, deleteUseCase)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals("Debe haber eliminado las 2 tareas en TaskRepository", 0, taskRepo.tasksFlow.value.size)
        assertEquals("Debe haber llamado 2 veces a deleteTask", 2, taskRepo.deleteCalls)
        assertTrue("Debe invocar resetUserData en remoteDataSource", remoteDataSource.resetUserDataCalled)
    }

    @Test
    fun eliminarCuentaEliminaTareasEnTaskRepositoryYSincronizaConRemoteDataSource() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_abc", "user@krono.app")
        val initialTasks = listOf(
            Task(id = "task-1", title = "Tarea 1"),
        )
        val taskRepo = NoOpTaskRepository(initialTasks)
        val remoteDataSource = NoOpProfileRemoteDataSource()

        val resetUseCase = ResetUserDataUseCase(
            localStore = store,
            taskRepository = taskRepo,
            remoteDataSource = remoteDataSource,
        )
        val deleteUseCase = DeleteAccountUseCase(
            localStore = store,
            taskRepository = taskRepo,
            remoteDataSource = remoteDataSource,
        )
        val vm = SecurityZoneViewModel(resetUseCase, deleteUseCase)

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onReauthPasswordChange("Password123*")
        vm.onConfirmDeleteAccount()
        advanceUntilIdle()

        assertEquals("Debe haber eliminado la tarea", 0, taskRepo.tasksFlow.value.size)
        assertEquals(1, taskRepo.deleteCalls)
        assertTrue("Debe invocar deleteAccount en remoteDataSource", remoteDataSource.deleteAccountCalled)
        assertEquals("Password123*", remoteDataSource.lastDeletePassword)
    }

    @Test
    fun restablecerDatosConFalloRemotoMuestraErrorYConservaElAlmacenLocal() = runTest {
        val store = ProfileLocalStore()
        store.saveProfile("Usuario Modificado", "Rol Modificado")
        val taskRepo = object : NoOpTaskRepository() {
            override suspend fun deleteTask(taskId: String): KronoResult<Unit> =
                KronoResult.Error(RuntimeException("PERMISSION_DENIED"))
        }
        taskRepo.tasksFlow.value = listOf(Task(id = "task-1", title = "Tarea 1"))
        val resetUseCase = ResetUserDataUseCase(
            localStore = store,
            taskRepository = taskRepo,
            remoteDataSource = NoOpProfileRemoteDataSource(),
        )
        val deleteUseCase = DeleteAccountUseCase(store)
        val vm = SecurityZoneViewModel(resetUseCase, deleteUseCase)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals("Usuario Modificado", store.userName.value)
        assertEquals("Rol Modificado", store.userRole.value)
        assertFalse(vm.uiState.value.isResettingData)
        assertNull(vm.uiState.value.successMessage)
        assertEquals("PERMISSION_DENIED", vm.uiState.value.errorMessage)
    }
}
