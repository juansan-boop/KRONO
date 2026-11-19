package com.krono.feature.profile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.krono.feature.profile.domain.NotificationPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [ProfileLocalStore] que validan:
 * - El almacenamiento persistente y el aislamiento estricto por `uid` con DataStore.
 * - La no colisión ni sobreescritura entre múltiples usuarios (`userA` vs `userB`).
 * - La reactividad inmediata de los flujos [kotlinx.coroutines.flow.StateFlow].
 * - La recuperación de preferencias al reiniciar la instancia vinculada a DataStore.
 * - El restablecimiento a valores de fábrica ([ProfileLocalStore.resetToDefaults]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileLocalStoreTest {

    private lateinit var store: ProfileLocalStore

    @Before
    fun setUp() {
        store = ProfileLocalStore()
    }

    // =========================================================================
    // Vinculación y derivación de nombre
    // =========================================================================

    /**
     * Verifica que bindUser vincule el uid y derive el nombre visible a partir del correo
     * cuando no existe nombre previo guardado.
     */
    @Test
    fun bindUser_derivaNombreDesdeCorreoCuandoNoHayNombreGuardado() {
        store.bindUser("user_abc", "sofia.vergara@krono.app")

        assertEquals("user_abc", store.activeUserId)
        assertEquals("sofia.vergara@krono.app", store.userEmail.value)
        assertEquals("Sofia Vergara", store.userName.value)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
    }

    /**
     * Verifica que saveProfile actualice los StateFlows inmediatamente en memoria.
     */
    @Test
    fun saveProfile_actualizaValoresEnMemoriaYStateFlows() {
        store.bindUser("user_1", "ana@krono.app")
        store.saveProfile(name = "Ana Gómez", role = "Estudiante", avatarUri = "file:///avatar.png")

        assertEquals("Ana Gómez", store.userName.value)
        assertEquals("Estudiante", store.userRole.value)
        assertEquals("file:///avatar.png", store.avatarUri.value)
    }

    // =========================================================================
    // Aislamiento por UID (userA vs userB) en memoria
    // =========================================================================

    /**
     * Verifica que los cambios y configuraciones de un usuario no afecten ni colisionen
     * con los de otro usuario en memoria.
     */
    @Test
    fun aislamientoPorUid_cambiosEnUnUsuarioNoAfectanAOtroUsuario() {
        // Usuario A guarda sus datos personalizados
        store.bindUser("userA", "userA@krono.app")
        store.saveProfile(name = "Usuario A", role = "Trabajador tiempo completo", avatarUri = "file:///avatarA.png")
        store.setDynamicBreaksEnabled(false)
        store.setSilenceInternalNotificationsEnabled(false)
        store.setPriorityAlertsEnabled(false)
        store.saveNotificationPrefs(
            NotificationPrefs(
                deadlineReminderMinutes = 30,
                deadlineRemindersEnabled = false,
                focusStartSound = false,
                soundTone = "Campana",
            )
        )

        // Usuario B se autentica y debe tener valores limpios e independientes
        store.bindUser("userB", "carlos.mendoza@krono.app")
        assertEquals("userB", store.activeUserId)
        assertEquals("Carlos Mendoza", store.userName.value)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
        assertNull(store.avatarUri.value)
        assertTrue(store.dynamicBreaksEnabled.value)
        assertTrue(store.silenceInternalNotificationsEnabled.value)
        assertTrue(store.priorityAlertsEnabled.value)
        assertEquals(NotificationPrefs.DEFAULT_REMINDER_MINUTES, store.deadlineReminderMinutes.value)
        assertTrue(store.deadlineRemindersEnabled.value)
        assertTrue(store.focusStartSound.value)
        assertEquals(NotificationPrefs.DEFAULT_SOUND_TONE, store.soundTone.value)

        // Usuario B guarda su propia información
        store.saveProfile(name = "Carlos M.", role = "Estudiante", avatarUri = "file:///avatarB.png")
        store.setDynamicBreaksEnabled(true)

        // Volvemos al Usuario A y verificamos que sus datos se conservaron al 100%
        store.bindUser("userA", "userA@krono.app")
        assertEquals("userA", store.activeUserId)
        assertEquals("Usuario A", store.userName.value)
        assertEquals("Trabajador tiempo completo", store.userRole.value)
        assertEquals("file:///avatarA.png", store.avatarUri.value)
        assertFalse(store.dynamicBreaksEnabled.value)
        assertFalse(store.silenceInternalNotificationsEnabled.value)
        assertFalse(store.priorityAlertsEnabled.value)
        assertEquals(30, store.deadlineReminderMinutes.value)
        assertFalse(store.deadlineRemindersEnabled.value)
        assertFalse(store.focusStartSound.value)
        assertEquals("Campana", store.soundTone.value)
    }

    // =========================================================================
    // Emisión reactiva StateFlow (Turbine)
    // =========================================================================

    /**
     * Verifica que los StateFlows emitan reactivamente los nuevos valores ante llamadas a saveProfile.
     */
    @Test
    fun emisionReactiva_saveProfile_emiteEnStateFlows() = runTest {
        store.bindUser("user_reactivo", "reactivo@krono.app")

        store.userName.test {
            assertEquals("Reactivo", awaitItem())
            store.saveProfile("Nombre Modificado", "Rol Modificado")
            assertEquals("Nombre Modificado", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        store.userRole.test {
            assertEquals("Rol Modificado", awaitItem())
            store.saveProfile("Nombre Modificado", "Estudiante")
            assertEquals("Estudiante", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica la emisión reactiva de los interruptores de ajustes del sistema.
     */
    @Test
    fun emisionReactiva_interruptoresDeAjustes_emitenCambios() = runTest {
        store.bindUser("user_toggles", "toggles@krono.app")

        store.dynamicBreaksEnabled.test {
            assertTrue(awaitItem())
            store.setDynamicBreaksEnabled(false)
            assertFalse(awaitItem())
            store.setDynamicBreaksEnabled(true)
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        store.silenceInternalNotificationsEnabled.test {
            assertTrue(awaitItem())
            store.setSilenceInternalNotificationsEnabled(false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        store.priorityAlertsEnabled.test {
            assertTrue(awaitItem())
            store.setPriorityAlertsEnabled(false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifica la emisión reactiva de las preferencias de notificación y alarmas.
     */
    @Test
    fun emisionReactiva_saveNotificationPrefs_emiteCambiosEnFlujos() = runTest {
        store.bindUser("user_notifs", "notifs@krono.app")

        store.deadlineReminderMinutes.test {
            assertEquals(NotificationPrefs.DEFAULT_REMINDER_MINUTES, awaitItem())

            val custom = NotificationPrefs(
                deadlineReminderMinutes = 15,
                deadlineRemindersEnabled = false,
                focusStartSound = false,
                soundTone = "Alerta Suave",
            )
            store.saveNotificationPrefs(custom)

            assertEquals(15, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(15, store.deadlineReminderMinutes.value)
        assertFalse(store.deadlineRemindersEnabled.value)
        assertFalse(store.focusStartSound.value)
        assertEquals("Alerta Suave", store.soundTone.value)
        assertEquals(15, store.getNotificationPrefs().deadlineReminderMinutes)
    }

    /**
     * Verifica que saveAvatarUri actualice y emita reactivamente null o nueva URI.
     */
    @Test
    fun emisionReactiva_saveAvatarUri_actualizaCorrectamente() = runTest {
        store.bindUser("user_avatar", "avatar@krono.app")

        store.avatarUri.test {
            assertNull(awaitItem())
            store.saveAvatarUri("content://media/avatar_1.png")
            assertEquals("content://media/avatar_1.png", awaitItem())
            store.saveAvatarUri(null)
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // Aislamiento y persistencia con Preferences DataStore compartido
    // =========================================================================

    /**
     * Verifica que cuando se utiliza una instancia reactiva de [androidx.datastore.core.DataStore]:
     * 1. Las preferencias de userA y userB se persistan aisladas por prefijo uid.
     * 2. Ningún dato de userA se sobreescriba o colisione con userB.
     * 3. Al instanciar un nuevo [ProfileLocalStore] conectado al mismo DataStore,
     *    se restauren reactivamente los valores persistidos de cada usuario.
     */
    @Test
    fun persistenciaDataStore_aislamientoPorUidEntreInstancias() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val inMemoryDataStore = InMemoryPreferencesDataStore()

        val persistentStore1 = ProfileLocalStore(
            dataStore = inMemoryDataStore,
            coroutineScope = backgroundScope,
            ioDispatcher = testDispatcher,
        )

        // 1. Configuramos y guardamos datos para UserA
        persistentStore1.bindUser("userA", "userA@krono.app")
        advanceUntilIdle()

        persistentStore1.saveProfile(name = "Alice Wonder", role = "Estudiante", avatarUri = "file:///alice.png")
        persistentStore1.setDynamicBreaksEnabled(false)
        persistentStore1.setSilenceInternalNotificationsEnabled(false)
        persistentStore1.saveNotificationPrefs(
            NotificationPrefs(
                deadlineReminderMinutes = 20,
                deadlineRemindersEnabled = false,
                focusStartSound = false,
                soundTone = "Campanada",
            )
        )
        advanceUntilIdle()

        // 2. Configuramos y guardamos datos para UserB
        persistentStore1.bindUser("userB", "userB@krono.app")
        advanceUntilIdle()

        persistentStore1.saveProfile(name = "Bob Builder", role = "Trabajador tiempo completo")
        persistentStore1.setDynamicBreaksEnabled(true)
        persistentStore1.setSilenceInternalNotificationsEnabled(true)
        persistentStore1.saveNotificationPrefs(
            NotificationPrefs(
                deadlineReminderMinutes = 50,
                deadlineRemindersEnabled = true,
                focusStartSound = true,
                soundTone = "Zen",
            )
        )
        advanceUntilIdle()

        // 3. Simulamos reinicio de la aplicación instanciando un nuevo ProfileLocalStore
        // sobre el MISMO DataStore compartido
        val persistentStore2 = ProfileLocalStore(
            dataStore = inMemoryDataStore,
            coroutineScope = backgroundScope,
            ioDispatcher = testDispatcher,
        )

        // 4. Vinculamos UserA en la nueva instancia y verificamos que recupera sus datos intactos
        persistentStore2.bindUser("userA", "userA@krono.app")
        advanceUntilIdle()

        assertEquals("Alice Wonder", persistentStore2.userName.value)
        assertEquals("Estudiante", persistentStore2.userRole.value)
        assertEquals("file:///alice.png", persistentStore2.avatarUri.value)
        assertFalse(persistentStore2.dynamicBreaksEnabled.value)
        assertFalse(persistentStore2.silenceInternalNotificationsEnabled.value)
        assertEquals(20, persistentStore2.deadlineReminderMinutes.value)
        assertFalse(persistentStore2.deadlineRemindersEnabled.value)
        assertEquals("Campanada", persistentStore2.soundTone.value)

        // 5. Vinculamos UserB en la nueva instancia y verificamos que recupera sus datos sin colisión
        persistentStore2.bindUser("userB", "userB@krono.app")
        advanceUntilIdle()

        assertEquals("Bob Builder", persistentStore2.userName.value)
        assertEquals("Trabajador tiempo completo", persistentStore2.userRole.value)
        assertNull(persistentStore2.avatarUri.value)
        assertTrue(persistentStore2.dynamicBreaksEnabled.value)
        assertTrue(persistentStore2.silenceInternalNotificationsEnabled.value)
        assertEquals(50, persistentStore2.deadlineReminderMinutes.value)
        assertTrue(persistentStore2.deadlineRemindersEnabled.value)
        assertEquals("Zen", persistentStore2.soundTone.value)

        // Verificamos que los datos de userA y userB son mutuamente disjuntos
        assertNotEquals(persistentStore2.userName.value, "Alice Wonder")
    }

    // =========================================================================
    // Restablecimiento a valores por defecto (resetToDefaults)
    // =========================================================================

    /**
     * Verifica que resetToDefaults limpie las preferencias del usuario activo, restaure
     * los valores por defecto del sistema y derive el nombre a partir del correo.
     */
    @Test
    fun resetToDefaults_restableceTodosLosAjustesYConservaCorreo() {
        store.bindUser("user_1", "maria.perez@krono.app")
        store.saveProfile("María Personalizado", "Rol Personalizado", "file:///foto.jpg")
        store.setDynamicBreaksEnabled(false)
        store.saveNotificationPrefs(
            NotificationPrefs(
                deadlineReminderMinutes = 60,
                deadlineRemindersEnabled = false,
                focusStartSound = false,
                soundTone = "Alarma",
            )
        )

        store.resetToDefaults()

        assertEquals("Maria Perez", store.userName.value)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
        assertNull(store.avatarUri.value)
        assertTrue(store.dynamicBreaksEnabled.value)
        assertTrue(store.silenceInternalNotificationsEnabled.value)
        assertTrue(store.priorityAlertsEnabled.value)
        assertEquals(NotificationPrefs.DEFAULT_REMINDER_MINUTES, store.deadlineReminderMinutes.value)
        assertTrue(store.deadlineRemindersEnabled.value)
        assertTrue(store.focusStartSound.value)
        assertEquals(NotificationPrefs.DEFAULT_SOUND_TONE, store.soundTone.value)
    }

    // =========================================================================
    // Funciones auxiliares
    // =========================================================================

    /**
     * Verifica que formatNameFromEmail procese correos con diversos separadores
     * convirtiéndolos a nombres legibles en Title Case.
     */
    @Test
    fun formatNameFromEmail_manejaFormatosDiversos() {
        assertEquals("Juan Esteban", ProfileLocalStore.formatNameFromEmail("juan.esteban@krono.app"))
        assertEquals("Maria De Los Angeles", ProfileLocalStore.formatNameFromEmail("maria_de_los_angeles@krono.app"))
        assertEquals("Carlos Perez", ProfileLocalStore.formatNameFromEmail("carlos-perez@krono.app"))
        assertEquals("Admin", ProfileLocalStore.formatNameFromEmail("admin@krono.app"))
        assertEquals("", ProfileLocalStore.formatNameFromEmail(""))
        assertEquals("", ProfileLocalStore.formatNameFromEmail("  @krono.app"))
    }
}

/**
 * Implementación de soporte en memoria para pruebas de [DataStore] de [Preferences] que emite
 * reactivamente las mutaciones garantizando aislamiento y persistencia determinista.
 */
class InMemoryPreferencesDataStore(
    initialPreferences: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initialPreferences)

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
