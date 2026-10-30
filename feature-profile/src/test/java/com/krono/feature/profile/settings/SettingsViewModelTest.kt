package com.krono.feature.profile.settings

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.domain.auth.User
import com.krono.feature.profile.MainDispatcherRule
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.krono.core.domain.auth.ObserveSessionUseCase

/**
 * Doble de prueba en memoria para [AuthRepository] que soporta compuertas de suspensión ([gate])
 * durante la ejecución de [logout].
 */
private class TestAuthRepository : AuthRepository {
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var logoutCalls = 0
        private set
    val sessionFlow = MutableStateFlow<User?>(null)

    override fun observeSession(): Flow<User?> = sessionFlow
    override suspend fun login(email: String, password: String): KronoResult<User> = KronoResult.Success(User("id", email))
    override suspend fun register(email: String, password: String): KronoResult<User> = KronoResult.Success(User("id", email))
    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> = KronoResult.Success(Unit)

    override suspend fun logout(): KronoResult<Unit> {
        logoutCalls++
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return KronoResult.Success(Unit)
    }
}

/**
 * Pruebas unitarias para [SettingsViewModel] (F-19, F-20, F-25 CA-4).
 *
 * Valida de forma aislada:
 * - El estado inicial del hub de ajustes y perfil.
 * - La alternancia de interruptores (pausas dinámicas, silenciar notificaciones, alertas P1).
 * - La apertura y cierre seguro del diálogo de confirmación de cierre de sesión.
 * - La ejecución de [LogoutUseCase], manejo de carga [SettingsUiState.isLoggingOut] y emisión de [SettingsEvent.LoggedOut].
 * - El manejo de errores y la protección contra doble clic concurrente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = TestAuthRepository()

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(LogoutUseCase(authRepository))

    @Test
    fun elEstadoInicialReflejaLosValoresPorDefecto() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertEquals("Usuario", state.userName)
        assertEquals("", state.userEmail)
        assertEquals("Estudiante y trabajador", state.userRole)
        assertNull(state.avatarUri)
        assertTrue(state.dynamicBreaksEnabled)
        assertTrue(state.silenceInternalNotificationsEnabled)
        assertTrue(state.priorityAlertsEnabled)
        assertFalse(state.showLogoutConfirmationDialog)
        assertFalse(state.isLoggingOut)
        assertNull(state.logoutError)
    }

    @Test
    fun modificarInterruptoresActualizaElEstadoCorrectamente() {
        val vm = createViewModel()

        vm.onDynamicBreaksToggle(false)
        assertFalse(vm.uiState.value.dynamicBreaksEnabled)

        vm.onDynamicBreaksToggle(true)
        assertTrue(vm.uiState.value.dynamicBreaksEnabled)

        vm.onSilenceNotificationsToggle(false)
        assertFalse(vm.uiState.value.silenceInternalNotificationsEnabled)

        vm.onSilenceNotificationsToggle(true)
        assertTrue(vm.uiState.value.silenceInternalNotificationsEnabled)

        vm.onPriorityAlertsToggle(false)
        assertFalse(vm.uiState.value.priorityAlertsEnabled)

        vm.onPriorityAlertsToggle(true)
        assertTrue(vm.uiState.value.priorityAlertsEnabled)
    }

    @Test
    fun solicitarCierreDeSesionAbreElDialogoYLimpiaErroresPrevios() {
        val vm = createViewModel()

        vm.onRequestLogout()

        val state = vm.uiState.value
        assertTrue(state.showLogoutConfirmationDialog)
        assertNull(state.logoutError)
    }

    @Test
    fun descartarElDialogoOcultaElDialogoYLimpiaErrores() {
        val vm = createViewModel()

        vm.onRequestLogout()
        assertTrue(vm.uiState.value.showLogoutConfirmationDialog)

        vm.onDismissLogoutDialog()
        assertFalse(vm.uiState.value.showLogoutConfirmationDialog)
        assertNull(vm.uiState.value.logoutError)
    }

    @Test
    fun descartarElDialogoDuranteCierreDeSesionEsIgnorado() = runTest {
        val gate = CompletableDeferred<Unit>()
        authRepository.gate = gate
        val vm = createViewModel()

        vm.onRequestLogout()
        vm.onConfirmLogout()

        // Operación en progreso antes de completar el gate
        assertTrue(vm.uiState.value.isLoggingOut)
        assertTrue(vm.uiState.value.showLogoutConfirmationDialog)

        // Intento de descartar cancelado por seguridad
        vm.onDismissLogoutDialog()
        assertTrue(vm.uiState.value.showLogoutConfirmationDialog)

        // Liberar operación
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoggingOut)
        assertFalse(vm.uiState.value.showLogoutConfirmationDialog)
    }

    @Test
    fun confirmarCierreDeSesionExitosoEmiteEventoDeNavegacionYCierraDialogo() = runTest {
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.events.test {
            vm.onConfirmLogout()
            assertEquals(SettingsEvent.LoggedOut, awaitItem())
        }

        val state = vm.uiState.value
        assertFalse(state.isLoggingOut)
        assertFalse(state.showLogoutConfirmationDialog)
        assertNull(state.logoutError)
    }

    @Test
    fun confirmarCierreDeSesionConErrorMantieneDialogoYExponeMensaje() = runTest {
        authRepository.failure = AuthError.NoConnection
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.onConfirmLogout()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoggingOut)
        assertTrue(state.showLogoutConfirmationDialog)
        assertEquals(LogoutError.NO_CONNECTION, state.logoutError)
    }


    @Test
    fun confirmarCierreDeSesionConDemasiadosIntentosExponeErrorTipado() = runTest {
        authRepository.failure = AuthError.TooManyRequests
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.onConfirmLogout()
        advanceUntilIdle()

        assertEquals(LogoutError.TOO_MANY_REQUESTS, vm.uiState.value.logoutError)
    }

    @Test
    fun confirmarCierreDeSesionNoExponeElMensajeCrudoDeLaExcepcion() = runTest {
        authRepository.failure = RuntimeException("An internal error has occurred")
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.onConfirmLogout()
        advanceUntilIdle()

        assertEquals(LogoutError.UNEXPECTED, vm.uiState.value.logoutError)
    }
    @Test
    fun confirmarCierreDeSesionConErrorInesperadoExponeMensajeGenerico() = runTest {
        authRepository.failure = RuntimeException("")
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.onConfirmLogout()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoggingOut)
        assertTrue(state.showLogoutConfirmationDialog)
        assertEquals(LogoutError.UNEXPECTED, state.logoutError)
    }

    @Test
    fun confirmarCierreDeSesionEvitaDobleEjecucionConcurrente() = runTest {
        val gate = CompletableDeferred<Unit>()
        authRepository.gate = gate
        val vm = createViewModel()
        vm.onRequestLogout()

        vm.events.test {
            vm.onConfirmLogout()
            assertTrue(vm.uiState.value.isLoggingOut)

            // Segundo intento concurrente mientras el primero sigue bloqueado
            vm.onConfirmLogout()

            // Liberamos compuerta
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(SettingsEvent.LoggedOut, awaitItem())
            expectNoEvents()
        }

        assertEquals(1, authRepository.logoutCalls)
        assertFalse(vm.uiState.value.isLoggingOut)
        assertFalse(vm.uiState.value.showLogoutConfirmationDialog)
    }

    @Test
    fun modificarInterruptoresPersisteValoresEnProfileLocalStore() {
        val store = ProfileLocalStore()
        val vm = SettingsViewModel(LogoutUseCase(authRepository), store)

        vm.onDynamicBreaksToggle(false)
        assertFalse(store.dynamicBreaksEnabled.value)
        assertFalse(vm.uiState.value.dynamicBreaksEnabled)

        vm.onSilenceNotificationsToggle(false)
        assertFalse(store.silenceInternalNotificationsEnabled.value)
        assertFalse(vm.uiState.value.silenceInternalNotificationsEnabled)

        vm.onPriorityAlertsToggle(false)
        assertFalse(store.priorityAlertsEnabled.value)
        assertFalse(vm.uiState.value.priorityAlertsEnabled)
    }

    @Test
    fun cambioEnProfileLocalStoreActualizaAutomaticamenteElEstadoVisual() = runTest {
        val store = ProfileLocalStore()
        val vm = SettingsViewModel(LogoutUseCase(authRepository), store)

        store.saveProfile("Carlos Sainz", "Piloto F1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Carlos Sainz", state.userName)
        assertEquals("Piloto F1", state.userRole)
        assertEquals("CS", state.userInitials)
    }

    @Test
    fun sesionDeUsuarioAutenticadoActualizaEmailYNombreEnSettings() = runTest {
        val store = ProfileLocalStore()
        val vm = SettingsViewModel(
            logout = LogoutUseCase(authRepository),
            observeSession = ObserveSessionUseCase(authRepository),
            localStore = store,
        )

        authRepository.sessionFlow.value = User(id = "user_789", email = "maria.gonzalez@empresa.com")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Maria Gonzalez", state.userName)
        assertEquals("maria.gonzalez@empresa.com", state.userEmail)
        assertEquals("MG", state.userInitials)
    }

    @Test
    fun cambioDeAvatarEnProfileLocalStoreActualizaAvatarUriEnSettings() = runTest {
        val store = ProfileLocalStore()
        val vm = SettingsViewModel(LogoutUseCase(authRepository), store)

        store.saveAvatarUri("content://media/picker/photo123")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("content://media/picker/photo123", state.avatarUri)
    }

    @Test
    fun alternarInterruptoresSincronizaConRemoteDataSource() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_sync_test", "sync@krono.app")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val vm = SettingsViewModel(
            logout = LogoutUseCase(authRepository),
            observeSession = ObserveSessionUseCase(authRepository),
            localStore = store,
            remoteDataSource = remoteDataSource,
        )

        vm.onDynamicBreaksToggle(false)
        vm.onSilenceNotificationsToggle(false)
        vm.onPriorityAlertsToggle(false)
        advanceUntilIdle()

        assertFalse(store.dynamicBreaksEnabled.value)
        assertFalse(store.silenceInternalNotificationsEnabled.value)
        assertFalse(store.priorityAlertsEnabled.value)
    }

    @Test
    fun sesionDeUsuarioAutenticadoInvocaSyncFromRemoteEnSettings() = runTest {
        val store = ProfileLocalStore()
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val vm = SettingsViewModel(
            logout = LogoutUseCase(authRepository),
            observeSession = ObserveSessionUseCase(authRepository),
            localStore = store,
            remoteDataSource = remoteDataSource,
        )

        authRepository.sessionFlow.value = User(id = "user_remote_sync_456", email = "test@krono.app")
        advanceUntilIdle()

        assertTrue(remoteDataSource.syncFromRemoteCalled)
        assertEquals("user_remote_sync_456", remoteDataSource.lastSyncUserId)
    }
}

