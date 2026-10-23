package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [FirestoreProfileDataSource].
 * Valida tolerancia a fallos, ejecución segura en entornos locales sin Firebase inicializado
 * y la construcción correcta del documento inicial (bootstrap) en Cloud Firestore (D-1, F-21).
 */
class FirestoreProfileDataSourceTest {

    @Test
    fun createBootstrapPayload_conDisplayName_separaNombreYApellidoYAsignaDefaults() {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
            serverTimestampProvider = { "MOCK_TIMESTAMP" },
        )

        val payload = dataSource.createBootstrapPayload(
            email = "juan.perez@krono.app",
            displayName = "Juan Perez",
        )

        assertEquals("Juan", payload[FirestoreProfileDataSource.FIELD_NAME])
        assertEquals("Perez", payload[FirestoreProfileDataSource.FIELD_LAST_NAME])
        assertEquals("juan.perez@krono.app", payload[FirestoreProfileDataSource.FIELD_EMAIL])
        assertEquals("Estudiante y trabajador", payload[FirestoreProfileDataSource.FIELD_ROLE])
        assertNull(payload[FirestoreProfileDataSource.FIELD_AVATAR_URI])
        assertEquals(30, payload[FirestoreProfileDataSource.FIELD_DEADLINE_MINUTES])
        assertEquals(true, payload[FirestoreProfileDataSource.FIELD_FOCUS_SOUND])
        assertEquals("Chime", payload[FirestoreProfileDataSource.FIELD_SOUND_TONE])
        assertEquals(true, payload[FirestoreProfileDataSource.FIELD_DEADLINE_ENABLED])
        assertEquals(true, payload[FirestoreProfileDataSource.FIELD_DYNAMIC_BREAKS])
        assertEquals(true, payload[FirestoreProfileDataSource.FIELD_SILENCE_NOTIFS])
        assertEquals(true, payload[FirestoreProfileDataSource.FIELD_PRIORITY_ALERTS])
        assertEquals("MOCK_TIMESTAMP", payload[FirestoreProfileDataSource.FIELD_UPDATED_AT])
    }

    @Test
    fun createBootstrapPayload_sinDisplayName_derivaNombreDesdeCorreo() {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
            serverTimestampProvider = { "MOCK_TIMESTAMP" },
        )

        val payload = dataSource.createBootstrapPayload(
            email = "carlos.alberto.gomez@krono.app",
            displayName = null,
        )

        assertEquals("Carlos", payload[FirestoreProfileDataSource.FIELD_NAME])
        assertEquals("Alberto Gomez", payload[FirestoreProfileDataSource.FIELD_LAST_NAME])
        assertEquals("carlos.alberto.gomez@krono.app", payload[FirestoreProfileDataSource.FIELD_EMAIL])
    }

    @Test
    fun createBootstrapPayload_conCamposExistentes_preservaValores() {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
            serverTimestampProvider = { "MOCK_TIMESTAMP" },
        )

        val payload = dataSource.createBootstrapPayload(
            email = "mateo@krono.app",
            displayName = "Mateo Ignorado",
            existingName = "MateoPersonalizado",
            existingLastName = "Silva",
            existingRole = "Estudiante",
        )

        assertEquals("MateoPersonalizado", payload[FirestoreProfileDataSource.FIELD_NAME])
        assertEquals("Silva", payload[FirestoreProfileDataSource.FIELD_LAST_NAME])
        assertEquals("Estudiante", payload[FirestoreProfileDataSource.FIELD_ROLE])
    }

    @Test
    fun saveUserProfile_conFirebaseNulo_noLanzaExcepcionYRetornaExito() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )

        val result = dataSource.saveUserProfile(
            userId = "user-123",
            name = "Carlos",
            role = "Estudiante",
            avatarUri = null,
        )

        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun saveNotificationPrefs_conFirebaseNulo_retornaExito() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )

        val result = dataSource.saveNotificationPrefs(
            userId = "user-123",
            prefs = NotificationPrefs(),
        )

        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun saveSettingsToggles_conFirebaseNulo_retornaExito() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )

        val result = dataSource.saveSettingsToggles(
            userId = "user-123",
            dynamicBreaks = true,
            silenceNotifications = true,
            priorityAlerts = false,
        )

        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun resetUserData_conFirebaseNulo_retornaExito() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )

        val result = dataSource.resetUserData("user-123")
        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun deleteAccount_sinSesionActiva_retornaExitoOErrorControladoSinCrash() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )

        val result = dataSource.deleteAccount("password123")
        assertTrue(result is KronoResult.Success)
    }

    @Test
    fun syncFromRemote_conFirebaseNulo_noLanzaExcepcionYRetornaExito() = runTest {
        val dataSource = FirestoreProfileDataSource(
            firestoreProvider = { null },
            authProvider = { null },
        )
        val store = ProfileLocalStore()

        val result = dataSource.syncFromRemote("user-123", store)
        assertTrue(result is KronoResult.Success)
    }
}
