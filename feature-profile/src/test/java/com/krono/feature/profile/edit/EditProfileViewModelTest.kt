package com.krono.feature.profile.edit

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.core.domain.auth.User
import com.krono.feature.profile.MainDispatcherRule
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpAuthRepository
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRepositoryImpl
import com.krono.feature.profile.domain.BlankRoleException
import com.krono.feature.profile.domain.ProfileRepository
import com.krono.feature.profile.domain.SyncState
import com.krono.feature.profile.domain.UpdateProfileUseCase
import com.krono.feature.profile.ui.ProfileSaveError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Doble de prueba en memoria para [UpdateProfileUseCase] que permite simular latencia
 * suspendida con compuertas ([gate]), registrar llamadas y simular fallos de persistencia.
 */
private class TestUpdateProfileUseCase(
    repository: ProfileRepository = ProfileRepositoryImpl(),
) : UpdateProfileUseCase(repository) {
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var callCount: Int = 0
        private set
    var lastSavedName: String? = null
        private set
    var lastSavedRole: String? = null
        private set
    var lastSavedAvatarUri: String? = null
        private set

    override suspend operator fun invoke(
        name: String,
        role: String,
        avatarUri: String?,
    ): KronoResult<Unit> {
        callCount++
        lastSavedName = name
        lastSavedRole = role
        lastSavedAvatarUri = avatarUri
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return super.invoke(name, role, avatarUri)
    }
}

/**
 * Pruebas unitarias para [EditProfileViewModel] (F-19 CA-2: Edición de Perfil).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val updateProfileUseCase = TestUpdateProfileUseCase()

    private fun createViewModel(): EditProfileViewModel =
        EditProfileViewModel(updateProfile = updateProfileUseCase)

    @Test
    fun elEstadoInicialReflejaLosValoresPorDefecto() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertEquals("Usuario", state.name)
        assertEquals("", state.email)
        assertEquals("Estudiante y trabajador", state.role)
        assertEquals(4, state.availableRoles.size)
        assertEquals(
            listOf(
                "Estudiante y trabajador",
                "Estudiante",
                "Trabajador tiempo completo",
                "Trabajador tiempo parcial",
            ),
            state.availableRoles,
        )
        assertEquals("U", state.userInitials)
        assertNull(state.avatarUri)
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertFalse(state.isNameMissing)
        assertNull(state.saveError)
        assertTrue(state.canSave)
        assertEquals(SyncState.CONFIRMED, state.syncState)
    }

    @Test
    fun actualizarNombreYRolActualizaElEstadoYCalculaNuevasIniciales() {
        val vm = createViewModel()

        vm.onNameChange("Carlos Sánchez")
        vm.onRoleChange("Profesional Técnico")

        val state = vm.uiState.value
        assertEquals("Carlos Sánchez", state.name)
        assertEquals("CS", state.userInitials)
        assertEquals("Profesional Técnico", state.role)
        assertFalse(state.isNameMissing)
        assertNull(state.saveError)
    }

    @Test
    fun guardarConNombreVacioGeneraErrorDeValidacionYNoInvocaPersistencia() {
        val vm = createViewModel()

        vm.onNameChange("   ")
        vm.onSave()

        val state = vm.uiState.value
        assertTrue(state.isNameMissing)
        assertFalse(state.isSaving)
        assertEquals(0, updateProfileUseCase.callCount)
    }

    @Test
    fun escribirNombreValidoLimpiaElErrorDeNombrePrevio() {
        val vm = createViewModel()

        vm.onNameChange("")
        vm.onSave()
        assertTrue(vm.uiState.value.isNameMissing)

        vm.onNameChange("Juan Sora")
        assertFalse(vm.uiState.value.isNameMissing)
    }

    @Test
    fun guardarBloqueaDobleEnvioMientrasSeEncuentraGuardando() = runTest {
        val gate = CompletableDeferred<Unit>()
        updateProfileUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onNameChange("Esteban Sora")
            vm.onSave()

            assertTrue(vm.uiState.value.isSaving)

            vm.onSave()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            expectNoEvents()
        }

        assertEquals(1, updateProfileUseCase.callCount)
        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun guardadoExitosoEmiteEventoProfileSaved() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onNameChange("Laura Rivas")
            vm.onRoleChange("Investigadora & Operadora")
            vm.onSave()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertTrue(state.isSuccess)
            assertEquals("Laura Rivas", updateProfileUseCase.lastSavedName)
            assertEquals("Investigadora & Operadora", updateProfileUseCase.lastSavedRole)

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fallaEnPersistenciaDesactivaIsSavingYExponeErrorTipadoSinTextoTecnico() = runTest {
        updateProfileUseCase.failure = RuntimeException("Error en Firestore de prueba")
        val vm = createViewModel()

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertFalse(state.isSuccess)
            assertEquals(ProfileSaveError.UNEXPECTED, state.saveError)

            expectNoEvents()
        }
    }

    @Test
    fun fallaDeValidacionDelCasoDeUsoExponeErrorDeDatosInvalidos() = runTest {
        updateProfileUseCase.failure = BlankRoleException()
        val vm = createViewModel()

        vm.onSave()
        advanceUntilIdle()

        assertEquals(ProfileSaveError.INVALID_DATA, vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun editarUnCampoDespuesDeUnErrorLimpiaElErrorDeGuardado() = runTest {
        updateProfileUseCase.failure = RuntimeException("x")
        val vm = createViewModel()
        vm.onSave()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.saveError)

        vm.onNameChange("Otro Nombre")

        assertNull(vm.uiState.value.saveError)
    }

    @Test
    fun cancelarEmiteEventoNavigateBackSinModificarDatos() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onCancel()
            advanceUntilIdle()

            assertEquals(EditProfileEvent.NavigateBack, awaitItem())
            assertEquals(0, updateProfileUseCase.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarPerfilPersisteDatosEnProfileRepository() = runTest {
        val store = ProfileLocalStore()
        val repo = ProfileRepositoryImpl(store)
        val updateUseCase = UpdateProfileUseCase(repo)
        val vm = EditProfileViewModel(updateUseCase, repo)

        vm.onNameChange("Beatriz Pinzón")
        vm.onRoleChange("Economista Senior")

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Beatriz Pinzón", repo.userName.value)
        assertEquals("Economista Senior", repo.userRole.value)
    }

    @Test
    fun onAvatarSelectedActualizaAvatarUriEnElEstado() {
        val vm = createViewModel()
        val uri = "content://media/picker/123"

        vm.onAvatarSelected(uri)

        val state = vm.uiState.value
        assertEquals(uri, state.avatarUri)
    }

    @Test
    fun guardadoExitosoPasaAvatarUriAlCasoDeUso() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onNameChange("Laura Rivas")
            vm.onRoleChange("Estudiante")
            vm.onAvatarSelected("content://media/picker/456")
            vm.onSave()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertTrue(state.isSuccess)
            assertEquals("Laura Rivas", updateProfileUseCase.lastSavedName)
            assertEquals("Estudiante", updateProfileUseCase.lastSavedRole)
            assertEquals("content://media/picker/456", updateProfileUseCase.lastSavedAvatarUri)

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sesionDeUsuarioAutenticadoActualizaEmailYNombreFormateado() = runTest {
        val sessionFlow = MutableStateFlow<User?>(null)
        val authRepo = object : AuthRepository by NoOpAuthRepository() {
            override fun observeSession(): Flow<User?> = sessionFlow
        }
        val store = ProfileLocalStore()
        val repo = ProfileRepositoryImpl(store)
        val observeSession = ObserveSessionUseCase(authRepo)
        val vm = EditProfileViewModel(updateProfileUseCase, observeSession, repo)

        sessionFlow.value = User(id = "user_456", email = "juan.esteban@correo.com")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Juan Esteban", state.name)
        assertEquals("juan.esteban@correo.com", state.email)
        assertEquals("JE", state.userInitials)
    }

    @Test
    fun guardarPerfilSincronizaConRemoteDataSource() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_999", "ana.torres@krono.app")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { true })
        val updateUseCase = UpdateProfileUseCase(repo)
        val vm = EditProfileViewModel(updateUseCase, repo)

        vm.onNameChange("Ana Torres")
        vm.onRoleChange("Trabajador tiempo completo")
        vm.onAvatarSelected("content://media/picker/photo_789")

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Ana Torres", remoteDataSource.lastSavedName)
        assertEquals("Trabajador tiempo completo", remoteDataSource.lastSavedRole)
        assertEquals("content://media/picker/photo_789", remoteDataSource.lastSavedAvatarUri)
        assertEquals(SyncState.CONFIRMED, vm.uiState.value.syncState)
    }

    @Test
    fun updateProfileToleraFalloRemotoYPersisteLocalmente() = runTest {
        val store = ProfileLocalStore()
        val remoteDataSource = object : NoOpProfileRemoteDataSource() {
            override suspend fun saveUserProfile(
                userId: String,
                name: String,
                role: String,
                avatarUri: String?,
            ): KronoResult<Unit> {
                throw RuntimeException("PERMISSION_DENIED")
            }
        }
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { true })
        val updateUseCase = UpdateProfileUseCase(repo)
        val result = updateUseCase("Carlos Gómez", "Estudiante", null)

        assertTrue("Debe retornar éxito incluso si Firestore falla", result is KronoResult.Success)
        assertEquals("Carlos Gómez", store.userName.value)
        assertEquals("Estudiante", store.userRole.value)
        assertEquals(SyncState.REJECTED, repo.syncState.value)
    }

    @Test
    fun guardarPerfilAplicaTrimANombreYRolAntesDeGuardar() = runTest {
        val store = ProfileLocalStore()
        val repo = ProfileRepositoryImpl(store)
        val updateUseCase = UpdateProfileUseCase(repo)
        val vm = EditProfileViewModel(updateUseCase, repo)

        vm.onNameChange("   Carlos Sánchez   ")
        vm.onRoleChange("   Ingeniero de Software   ")

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()

            assertEquals(EditProfileEvent.ProfileSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Carlos Sánchez", store.userName.value)
        assertEquals("Ingeniero de Software", store.userRole.value)
        assertEquals("Carlos Sánchez", vm.uiState.value.name)
        assertEquals("Ingeniero de Software", vm.uiState.value.role)
    }

    @Test
    fun reemisionDeMismoUsuarioEnObserveSessionNoPisaEdicionEnProgreso() = runTest {
        val sessionFlow = MutableStateFlow<User?>(null)
        val authRepo = object : AuthRepository by NoOpAuthRepository() {
            override fun observeSession(): Flow<User?> = sessionFlow
        }
        val store = ProfileLocalStore()
        val repo = ProfileRepositoryImpl(store)
        val observeSession = ObserveSessionUseCase(authRepo)
        val vm = EditProfileViewModel(updateProfileUseCase, observeSession, repo)

        sessionFlow.value = User(id = "user_456", email = "juan.esteban@correo.com")
        advanceUntilIdle()
        assertEquals("Juan Esteban", vm.uiState.value.name)

        vm.onNameChange("Juan Esteban Modificado")
        assertEquals("Juan Esteban Modificado", vm.uiState.value.name)

        sessionFlow.value = User(id = "user_456", email = "juan.esteban@correo.com")
        advanceUntilIdle()

        assertEquals("Juan Esteban Modificado", vm.uiState.value.name)
    }

    @Test
    fun retrySyncInvocaSincronizacionEnRepositorio() = runTest {
        val store = ProfileLocalStore().apply {
            bindUser("user_retry", "retry@krono.app")
            saveProfile("Usuario Retry", "Estudiante")
        }
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { true })
        val vm = EditProfileViewModel(
            updateProfile = UpdateProfileUseCase(repo),
            profileRepository = repo,
        )

        vm.retrySync()
        advanceUntilIdle()

        assertEquals("Usuario Retry", remoteDataSource.lastSavedName)
        assertEquals(SyncState.CONFIRMED, vm.uiState.value.syncState)
    }

    @Test
    fun uiStateReflejaEstadoPendingSyncCuandoGuardadoOcurreOfflineYReintentoLoRecuperaAConfirmed() = runTest {
        var isOnline = false
        val store = ProfileLocalStore().apply {
            bindUser("user_offline", "offline@krono.app")
        }
        val remoteDataSource = NoOpProfileRemoteDataSource()
        remoteDataSource.shouldFail = com.krono.feature.profile.domain.NetworkUnavailableException()
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { isOnline })
        val vm = EditProfileViewModel(
            updateProfile = UpdateProfileUseCase(repo),
            profileRepository = repo,
        )

        vm.onNameChange("Usuario Desconectado")
        vm.onRoleChange("Estudiante")
        vm.onSave()
        advanceUntilIdle()

        assertEquals(SyncState.PENDING_SYNC, vm.uiState.value.syncState)

        // Se restablece conexión y se ejecuta retrySync()
        isOnline = true
        remoteDataSource.shouldFail = null
        vm.retrySync()
        advanceUntilIdle()

        assertEquals(SyncState.CONFIRMED, vm.uiState.value.syncState)
        assertEquals("Usuario Desconectado", remoteDataSource.lastSavedName)
    }

    @Test
    fun uiStateReflejaEstadoRejectedAnteFalloRemotoYReintentoLoRecuperaAConfirmed() = runTest {
        val store = ProfileLocalStore().apply {
            bindUser("user_rejected", "rejected@krono.app")
        }
        val remoteDataSource = NoOpProfileRemoteDataSource()
        remoteDataSource.shouldFail = IllegalStateException("PERMISSION_DENIED_EN_FIRESTORE")
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { true })
        val vm = EditProfileViewModel(
            updateProfile = UpdateProfileUseCase(repo),
            profileRepository = repo,
        )

        vm.onNameChange("Usuario Rechazado")
        vm.onRoleChange("Trabajador tiempo completo")
        vm.onSave()
        advanceUntilIdle()

        assertEquals(SyncState.REJECTED, vm.uiState.value.syncState)

        // Se corrige el fallo remoto y se reintenta
        remoteDataSource.shouldFail = null
        vm.retrySync()
        advanceUntilIdle()

        assertEquals(SyncState.CONFIRMED, vm.uiState.value.syncState)
        assertEquals("Usuario Rechazado", remoteDataSource.lastSavedName)
    }

    @Test
    fun retrySyncMantienePendingSyncSiDispositivoSigueOffline() = runTest {
        val store = ProfileLocalStore().apply {
            bindUser("user_still_offline", "still_offline@krono.app")
            saveProfile("Sin Red", "Estudiante")
        }
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val repo = ProfileRepositoryImpl(store, remoteDataSource, ConnectivityChecker { false })
        val vm = EditProfileViewModel(
            updateProfile = UpdateProfileUseCase(repo),
            profileRepository = repo,
        )

        vm.retrySync()
        advanceUntilIdle()

        assertEquals(SyncState.PENDING_SYNC, vm.uiState.value.syncState)
    }
}
