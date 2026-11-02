package com.krono.feature.profile.notifications

import app.cash.turbine.test
import com.krono.core.common.KronoResult
import com.krono.feature.profile.MainDispatcherRule
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.domain.InvalidReminderMinutesException
import com.krono.feature.profile.domain.NotificationPrefs

import com.krono.feature.profile.domain.ResetNotificationPrefsUseCase
import com.krono.feature.profile.domain.SaveNotificationPrefsUseCase
import com.krono.feature.profile.ui.ProfileSaveError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas unitarias para [NotificationPreferencesViewModel] (F-15 CA-1, CA-2, CA-3).
 *
 * Cubre:
 * 1. Estado inicial con `deadlineReminderMinutes == 30` por defecto (CA-1).
 * 2. Cambio de tiempo de anticipación, sonido de enfoque y switches.
 * 3. "Restablecer valores predeterminados" regresa `deadlineReminderMinutes` a `30` y restaura valores base (CA-2).
 * 4. Guardado exitoso emite `PreferencesSaved`.
 * 5. Falla en guardado (`KronoResult.Error`) desactiva `isSaving` y expone mensaje de error.
 * 6. Bloqueo de doble clic concurrente (`CompletableDeferred`) mientras `isSaving == true`.
 * 7. Denegación del permiso de notificaciones (`onPermissionResult(false)`) actualiza el estado de UI sin lanzar excepciones ni bloquear la funcionalidad (CA-3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var saveNotificationPrefsUseCase: TestSaveNotificationPrefsUseCase
    private lateinit var resetNotificationPrefsUseCase: TestResetNotificationPrefsUseCase

    @Before
    fun setUp() {
        saveNotificationPrefsUseCase = TestSaveNotificationPrefsUseCase()
        resetNotificationPrefsUseCase = TestResetNotificationPrefsUseCase()
    }

    private fun createViewModel(): NotificationPreferencesViewModel {
        return NotificationPreferencesViewModel(
            saveNotificationPrefs = saveNotificationPrefsUseCase,
            resetNotificationPrefs = resetNotificationPrefsUseCase,
        )
    }

    @Test
    fun estadoInicialContieneValoresPorDefectoY30MinutosDeAnticipacion() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertEquals("CA-1: Anticipación por defecto debe ser 30 minutos", 30, state.deadlineReminderMinutes)
        assertTrue(state.deadlineRemindersEnabled)
        assertTrue(state.focusStartSound)
        assertEquals("Chime", state.soundTone)
        assertTrue(state.isPermissionGranted)
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertNull(state.saveError)
    }

    @Test
    fun actualizarTiempoDeAnticipacionSonidoYSwitches() {
        val vm = createViewModel()

        vm.onDeadlineReminderMinutesChange(60)
        vm.onDeadlineRemindersEnabledChange(false)
        vm.onFocusStartSoundChange(false)
        vm.onSoundToneChange("Discreet")

        val state = vm.uiState.value
        assertEquals(60, state.deadlineReminderMinutes)
        assertFalse(state.deadlineRemindersEnabled)
        assertFalse(state.focusStartSound)
        assertEquals("Discreet", state.soundTone)
        assertNull(state.saveError)
    }

    @Test
    fun restablecerValoresPredeterminadosRegresaA30MinutosYValoresBase() = runTest {
        val vm = createViewModel()

        // Modificamos a valores alternativos
        vm.onDeadlineReminderMinutesChange(15)
        vm.onDeadlineRemindersEnabledChange(false)
        vm.onFocusStartSoundChange(false)
        vm.onSoundToneChange("Pulse")

        vm.events.test {
            vm.onResetToDefaultsClick()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("CA-2: Debe restablecer a 30 minutos", 30, state.deadlineReminderMinutes)
            assertTrue(state.deadlineRemindersEnabled)
            assertTrue(state.focusStartSound)
            assertEquals("Chime", state.soundTone)
            assertNull(state.saveError)

            assertEquals(NotificationPreferencesEvent.DefaultsRestored, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardadoExitosoEmitePreferencesSaved() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.onDeadlineReminderMinutesChange(15)
            vm.onFocusStartSoundChange(true)
            vm.onSaveClick()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertTrue(state.isSuccess)
            assertNotNull(saveNotificationPrefsUseCase.lastSavedPrefs)
            assertEquals(15, saveNotificationPrefsUseCase.lastSavedPrefs?.deadlineReminderMinutes)
            assertTrue(saveNotificationPrefsUseCase.lastSavedPrefs?.focusStartSound == true)

            assertEquals(NotificationPreferencesEvent.PreferencesSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fallaEnGuardadoDesactivaIsSavingYExponeErrorTipadoSinTextoTecnico() = runTest {
        saveNotificationPrefsUseCase.failure = RuntimeException("Error en Firestore de prueba")
        val vm = createViewModel()

        vm.events.test {
            vm.onSaveClick()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertFalse(state.isSuccess)
            assertEquals(ProfileSaveError.UNEXPECTED, state.saveError)

            expectNoEvents()
        }
    }


    @Test
    fun fallaDeValidacionExponeErrorDeDatosInvalidos() = runTest {
        saveNotificationPrefsUseCase.failure = InvalidReminderMinutesException()
        val vm = createViewModel()

        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(ProfileSaveError.INVALID_DATA, vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
    }
    @Test
    fun bloqueoDeDobleClicConcurrenteMientrasIsSavingEstaActivo() = runTest {
        val gate = CompletableDeferred<Unit>()
        saveNotificationPrefsUseCase.gate = gate
        val vm = createViewModel()

        vm.events.test {
            vm.onSaveClick()

            // Primer clic: suspensión activa
            assertTrue("isSaving debe activarse", vm.uiState.value.isSaving)

            // Segundo clic concurrente: debe ser descartado
            vm.onSaveClick()

            // Liberamos la operación suspendida
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(NotificationPreferencesEvent.PreferencesSaved, awaitItem())
            expectNoEvents()
        }

        assertEquals("No debe invocar el caso de uso nuevamente si isSaving es true", 1, saveNotificationPrefsUseCase.callCount)
        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun denegacionDePermisoActualizaEstadoSinLanzarExcepcionNiBloquearPantalla() = runTest {
        val vm = createViewModel()

        // CA-3: Usuario deniega el permiso
        vm.onPermissionResult(false)

        val state = vm.uiState.value
        assertFalse("El permiso debe marcarse como no otorgado", state.isPermissionGranted)
        assertNull(state.saveError)

        // La pantalla sigue siendo 100% interactiva
        vm.onDeadlineReminderMinutesChange(10)
        assertEquals(10, vm.uiState.value.deadlineReminderMinutes)

        // Verificamos eventos de navegación y permiso
        vm.events.test {
            vm.onRequestPermissionClick()
            assertEquals(NotificationPreferencesEvent.RequestNotificationPermission, awaitItem())

            vm.onBackClick()
            assertEquals(NotificationPreferencesEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarPreferenciasPersisteValoresEnProfileLocalStore() = runTest {
        val store = ProfileLocalStore()
        val saveUseCase = SaveNotificationPrefsUseCase(store)
        val resetUseCase = ResetNotificationPrefsUseCase(store)
        val vm = NotificationPreferencesViewModel(saveUseCase, resetUseCase, store)

        vm.onDeadlineReminderMinutesChange(45)
        vm.onFocusStartSoundChange(false)
        vm.onSoundToneChange("Tono Galáctico")

        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(45, store.deadlineReminderMinutes.value)
        assertFalse(store.focusStartSound.value)
        assertEquals("Tono Galáctico", store.soundTone.value)

        // Restablecer valores predeterminados (F-15 CA-2)
        vm.onResetToDefaultsClick()
        advanceUntilIdle()

        assertEquals(NotificationPrefs.DEFAULT_REMINDER_MINUTES, store.deadlineReminderMinutes.value)
        assertTrue(store.focusStartSound.value)
        assertEquals(NotificationPrefs.DEFAULT_SOUND_TONE, store.soundTone.value)
    }

    @Test
    fun guardarPreferenciasSincronizaConRemoteDataSourceYRestablecerSincroniza() = runTest {
        val store = ProfileLocalStore()
        store.bindUser("user_777", "carlos@krono.app")
        val remoteDataSource = NoOpProfileRemoteDataSource()
        val saveUseCase = SaveNotificationPrefsUseCase(store, remoteDataSource)
        val resetUseCase = ResetNotificationPrefsUseCase(store, remoteDataSource)
        val vm = NotificationPreferencesViewModel(saveUseCase, resetUseCase, store)

        vm.onDeadlineReminderMinutesChange(45)
        vm.onFocusStartSoundChange(false)
        vm.onSoundToneChange("Discreet")

        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(45, remoteDataSource.lastSavedPrefs?.deadlineReminderMinutes)
        assertFalse(remoteDataSource.lastSavedPrefs?.focusStartSound == true)
        assertEquals("Discreet", remoteDataSource.lastSavedPrefs?.soundTone)

        // Restablecer
        vm.onResetToDefaultsClick()
        advanceUntilIdle()

        assertEquals(NotificationPrefs.DEFAULT_REMINDER_MINUTES, remoteDataSource.lastSavedPrefs?.deadlineReminderMinutes)
        assertTrue(remoteDataSource.lastSavedPrefs?.focusStartSound == true)
        assertEquals(NotificationPrefs.DEFAULT_SOUND_TONE, remoteDataSource.lastSavedPrefs?.soundTone)
    }

    @Test
    fun saveNotificationPrefsToleraFalloRemotoYPersisteLocalmente() = runTest {
        val store = ProfileLocalStore()
        val remoteDataSource = object : NoOpProfileRemoteDataSource() {
            override suspend fun saveNotificationPrefs(
                userId: String,
                prefs: NotificationPrefs,
            ): KronoResult<Unit> {
                throw RuntimeException("PERMISSION_DENIED")
            }
        }
        val saveUseCase = SaveNotificationPrefsUseCase(store, remoteDataSource)
        val result = saveUseCase(NotificationPrefs(deadlineReminderMinutes = 15, focusStartSound = false))

        assertTrue("Debe retornar éxito incluso si Firestore falla", result is KronoResult.Success)
        assertEquals(15, store.deadlineReminderMinutes.value)
        assertFalse(store.focusStartSound.value)
    }
}


/**
 * Doble de prueba para [SaveNotificationPrefsUseCase].
 */
private class TestSaveNotificationPrefsUseCase : SaveNotificationPrefsUseCase() {
    var gate: CompletableDeferred<Unit>? = null
    var failure: Throwable? = null
    var callCount = 0
    var lastSavedPrefs: NotificationPrefs? = null

    override suspend fun invoke(prefs: NotificationPrefs): KronoResult<Unit> {
        callCount++
        lastSavedPrefs = prefs
        gate?.await()
        failure?.let { return KronoResult.Error(it) }
        return KronoResult.Success(Unit)
    }
}

/**
 * Doble de prueba para [ResetNotificationPrefsUseCase].
 */
private class TestResetNotificationPrefsUseCase : ResetNotificationPrefsUseCase() {
    var failure: Throwable? = null
    var callCount = 0

    override suspend fun invoke(): KronoResult<NotificationPrefs> {
        callCount++
        failure?.let { return KronoResult.Error(it) }
        return KronoResult.Success(NotificationPrefs())
    }
}
