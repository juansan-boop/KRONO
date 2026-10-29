package com.krono.feature.profile.security

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.feature.profile.MainDispatcherRule
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.domain.DeleteAccountUseCase
import com.krono.feature.profile.domain.InvalidPasswordException
import com.krono.feature.profile.domain.NetworkUnavailableException
import com.krono.feature.profile.domain.RemoteTimeoutException
import com.krono.feature.profile.domain.ResetUserDataUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertFalse(state.isReauthPasswordEmpty)
        assertFalse(state.isPasswordVisible)
        assertNull(state.error)
        assertFalse(state.isResetSuccessful)
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
            assertTrue(state.isResetSuccessful)
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
            assertEquals(SecurityZoneError.Unexpected("Error en Firestore al purgar"), state.error)

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
        assertTrue(state.isReauthPasswordEmpty)
        assertFalse(state.isDeletingAccount)
        assertEquals(0, deleteAccountUseCase.callCount)
    }

    @Test
    fun escribirContrasenaValidaLimpiaErrorYAlternaVisibilidad() {
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onConfirmDeleteAccount()
        assertTrue(vm.uiState.value.isReauthPasswordEmpty)

        vm.onReauthPasswordChange("MiClaveSegura123!")
        assertFalse(vm.uiState.value.isReauthPasswordEmpty)
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
            assertEquals(SecurityZoneError.Unexpected("Credenciales de reautenticación inválidas"), state.error)

            expectNoEvents()
        }
    }

    @Test
    fun operacionEnCursoBloqueaDobleEnvio() = runTest {
        val gate = CompletableDeferred<Unit>()
        resetUserDataUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.RESET_DATA)
            vm.onContinueToConfirmation()
            vm.onConfirmResetData()

            assertTrue(vm.uiState.value.isResettingData)
            assertTrue(vm.uiState.value.isOperationInProgress)

            // Intento concurrente de volver a confirmar (doble clic)
            vm.onConfirmResetData()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(SecurityZoneEvent.DataResetSuccess, awaitItem())
            expectNoEvents()
        }

        assertEquals(1, resetUserDataUseCase.callCount)
        assertFalse(vm.uiState.value.isResettingData)
    }

    @Test
    fun cancelarDuranteElRestablecimientoDetieneLaOperacionYCierraElDialogo() = runTest {
        val gate = CompletableDeferred<Unit>()
        resetUserDataUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.RESET_DATA)
            vm.onContinueToConfirmation()
            vm.onConfirmResetData()
            assertTrue(vm.uiState.value.isResettingData)

            vm.onDismissResetDialog()
            // Si la operación siguiera viva, al liberar la compuerta terminaría con éxito.
            gate.complete(Unit)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isResettingData)
            assertFalse(state.showResetConfirmationDialog)
            assertFalse(state.isResetSuccessful)
            assertNull(state.error)
            expectNoEvents()
        }
    }

    @Test
    fun cancelarDuranteLaEliminacionDeCuentaDetieneLaOperacionYNoNavega() = runTest {
        val gate = CompletableDeferred<Unit>()
        deleteAccountUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
            vm.onContinueToConfirmation()
            vm.onReauthPasswordChange("Password123*")
            vm.onConfirmDeleteAccount()
            assertTrue(vm.uiState.value.isDeletingAccount)

            vm.onDismissDeleteDialog()
            gate.complete(Unit)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isDeletingAccount)
            assertFalse(state.showDeleteAccountDialog)
            assertNull(state.error)
            expectNoEvents()
        }
    }

    @Test
    fun contrasenaIncorrectaMantieneElDialogoAbiertoConElErrorParaLaUi() = runTest {
        deleteAccountUseCase.failure = InvalidPasswordException()
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onReauthPasswordChange("incorrecta")
        vm.onConfirmDeleteAccount()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("El diálogo debe seguir abierto para mostrar el error", state.showDeleteAccountDialog)
        assertFalse(state.isDeletingAccount)
        assertEquals(SecurityZoneError.WrongPassword, state.error)
    }

    @Test
    fun editarLaContrasenaTrasUnErrorLimpiaElError() = runTest {
        deleteAccountUseCase.failure = InvalidPasswordException()
        val vm = createViewModel()
        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onReauthPasswordChange("incorrecta")
        vm.onConfirmDeleteAccount()
        advanceUntilIdle()

        vm.onReauthPasswordChange("otra")

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun restablecerSinRedExponeNetworkUnavailableEnElDialogo() = runTest {
        resetUserDataUseCase.failure = NetworkUnavailableException()
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.showResetConfirmationDialog)
        assertFalse(state.isResettingData)
        assertEquals(SecurityZoneError.NetworkUnavailable, state.error)
    }

    @Test
    fun tiempoLimiteAgotadoExponeTimeoutYLiberaElBoton() = runTest {
        resetUserDataUseCase.failure = RemoteTimeoutException()
        val vm = createViewModel()

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals(SecurityZoneError.Timeout, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isResettingData)
    }

    @Test
    fun cerrarElDialogoTrasUnErrorLoLimpia() = runTest {
        resetUserDataUseCase.failure = RemoteTimeoutException()
        val vm = createViewModel()
        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        vm.onDismissResetDialog()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.showResetConfirmationDialog)
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
    fun restablecerDatosInvocaUnaSolaPurgaAtomicaEnElOrigenRemoto() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_abc", "user@krono.app")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val vm = realViewModel(store, remoteDataSource)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals(listOf(NoOpProfileRemoteDataSource.CALL_RESET_USER_DATA), remoteDataSource.calls)
        assertEquals("user_abc", remoteDataSource.lastResetUserId)
        assertTrue(vm.uiState.value.isResetSuccessful)
    }

    @Test
    fun eliminarCuentaReautenticaYLuegoBorraEnElOrigenRemoto() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_abc", "user@krono.app")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val vm = realViewModel(store, remoteDataSource)

        vm.onSelectAction(SecurityActionType.DELETE_ACCOUNT)
        vm.onContinueToConfirmation()
        vm.onReauthPasswordChange("Password123*")
        vm.onConfirmDeleteAccount()
        advanceUntilIdle()

        assertEquals(
            listOf(NoOpProfileRemoteDataSource.CALL_REAUTHENTICATE, NoOpProfileRemoteDataSource.CALL_DELETE_ACCOUNT),
            remoteDataSource.calls,
        )
        assertEquals("Password123*", remoteDataSource.lastDeletePassword)
    }

    @Test
    fun restablecerDatosSinRedNoTocaElOrigenRemotoNiElAlmacenLocalYMuestraElError() = runTest {
        val store = ProfileLocalStore()
        store.saveProfile("Usuario Modificado", "Rol Modificado")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val vm = realViewModel(store, remoteDataSource, online = false)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertTrue("No debe emitirse ninguna escritura sin red", remoteDataSource.calls.isEmpty())
        assertEquals("Usuario Modificado", store.userName.value)
        assertFalse(vm.uiState.value.isResettingData)
        assertFalse(vm.uiState.value.isResetSuccessful)
        assertEquals(SecurityZoneError.NetworkUnavailable, vm.uiState.value.error)
    }

    @Test
    fun restablecerDatosConFalloRemotoMuestraErrorYConservaElAlmacenLocal() = runTest {
        val store = ProfileLocalStore()
        store.saveProfile("Usuario Modificado", "Rol Modificado")
        val remoteDataSource = NoOpProfileRemoteDataSource().apply {
            shouldFail = RuntimeException("PERMISSION_DENIED")
        }
        val vm = realViewModel(store, remoteDataSource)

        vm.onSelectAction(SecurityActionType.RESET_DATA)
        vm.onContinueToConfirmation()
        vm.onConfirmResetData()
        advanceUntilIdle()

        assertEquals("Usuario Modificado", store.userName.value)
        assertEquals("Rol Modificado", store.userRole.value)
        assertFalse(vm.uiState.value.isResettingData)
        assertFalse(vm.uiState.value.isResetSuccessful)
        assertEquals(SecurityZoneError.Unexpected("PERMISSION_DENIED"), vm.uiState.value.error)
    }

    private fun realViewModel(
        store: ProfileLocalStore,
        remoteDataSource: NoOpProfileRemoteDataSource,
        online: Boolean = true,
    ): SecurityZoneViewModel {
        val connectivity = ConnectivityChecker { online }
        return SecurityZoneViewModel(
            ResetUserDataUseCase(store, remoteDataSource, connectivity),
            DeleteAccountUseCase(store, remoteDataSource, connectivity),
        )
    }
}
