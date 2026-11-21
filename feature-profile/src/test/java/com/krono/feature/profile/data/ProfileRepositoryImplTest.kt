package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NetworkUnavailableException
import com.krono.feature.profile.domain.NotificationPrefs
import com.krono.feature.profile.domain.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de [ProfileRepositoryImpl] que validan exhaustivamente los estados
 * de sincronización ([SyncState.CONFIRMED], [SyncState.PENDING_SYNC], [SyncState.REJECTED])
 * y el mecanismo de reintento ([ProfileRepositoryImpl.retrySync]).
 */
class ProfileRepositoryImplTest {

    private lateinit var localStore: ProfileLocalStore
    private lateinit var remoteDataSource: NoOpProfileRemoteDataSource
    private var isOnline = true
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setUp() {
        localStore = ProfileLocalStore().apply {
            bindUser("user_test", "tester@krono.app")
        }
        remoteDataSource = NoOpProfileRemoteDataSource()
        isOnline = true
        repository = ProfileRepositoryImpl(
            localStore = localStore,
            remoteDataSource = remoteDataSource,
            connectivityChecker = ConnectivityChecker { isOnline },
        )
    }

    @Test
    fun estadoInicialDeSincronizacionEsConfirmed() {
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }

    // =========================================================================
    // Estados de sincronización en updateProfile
    // =========================================================================

    @Test
    fun updateProfile_cuandoOnlineYRemotoExitoso_estableceEstadoConfirmed() = runTest {
        val result = repository.updateProfile("Usuario Nuevo", "Estudiante")

        assertTrue(result is KronoResult.Success)
        assertEquals("Usuario Nuevo", localStore.userName.value)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }

    @Test
    fun updateProfile_cuandoOffline_persisteLocalYEstableceEstadoPendingSync() = runTest {
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()

        val result = repository.updateProfile("Usuario Offline", "Trabajador")

        assertTrue(result is KronoResult.Success)
        assertEquals("Usuario Offline", localStore.userName.value)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun updateProfile_cuandoRemotoRechaza_persisteLocalYEstableceEstadoRejected() = runTest {
        remoteDataSource.shouldFail = IllegalStateException("PERMISSION_DENIED")

        val result = repository.updateProfile("Usuario Rechazado", "Investigador")

        assertTrue(result is KronoResult.Success)
        assertEquals("Usuario Rechazado", localStore.userName.value)
        assertEquals(SyncState.REJECTED, repository.syncState.value)
    }

    // =========================================================================
    // Estados de sincronización en syncSettingsToggles
    // =========================================================================

    @Test
    fun syncSettingsToggles_cuandoOnlineYRemotoExitoso_estableceEstadoConfirmed() = runTest {
        repository.setDynamicBreaksEnabled(false)
        val result = repository.syncSettingsToggles()

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }

    @Test
    fun syncSettingsToggles_cuandoOffline_estableceEstadoPendingSync() = runTest {
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()

        val result = repository.syncSettingsToggles()

        assertTrue(result is KronoResult.Error)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun syncSettingsToggles_cuandoRemotoFalla_estableceEstadoRejected() = runTest {
        remoteDataSource.shouldFail = IllegalStateException("FIRESTORE_ERROR")

        val result = repository.syncSettingsToggles()

        assertTrue(result is KronoResult.Error)
        assertEquals(SyncState.REJECTED, repository.syncState.value)
    }

    // =========================================================================
    // Estados de sincronización en preferencias de notificación
    // =========================================================================

    @Test
    fun saveNotificationPrefs_cuandoOnlineYExitoso_estableceEstadoConfirmed() = runTest {
        val prefs = NotificationPrefs(soundTone = "Campanas", deadlineReminderMinutes = 15)
        val result = repository.saveNotificationPrefs(prefs)

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }

    @Test
    fun saveNotificationPrefs_cuandoOffline_persisteLocalYEstablecePendingSync() = runTest {
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()

        val prefs = NotificationPrefs(soundTone = "Marimba")
        val result = repository.saveNotificationPrefs(prefs)

        assertTrue(result is KronoResult.Success)
        assertEquals("Marimba", localStore.soundTone.value)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun saveNotificationPrefs_cuandoRemotoFalla_persisteLocalYEstableceRejected() = runTest {
        remoteDataSource.shouldFail = RuntimeException("Error remoto")

        val prefs = NotificationPrefs(focusStartSound = false)
        val result = repository.saveNotificationPrefs(prefs)

        assertTrue(result is KronoResult.Success)
        assertFalse(localStore.focusStartSound.value)
        assertEquals(SyncState.REJECTED, repository.syncState.value)
    }

    @Test
    fun resetNotificationPrefs_cuandoOffline_establecePendingSync() = runTest {
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()

        val result = repository.resetNotificationPrefs()

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun resetNotificationPrefs_cuandoRemotoFalla_estableceRejected() = runTest {
        remoteDataSource.shouldFail = RuntimeException("Falla en reseteo")

        val result = repository.resetNotificationPrefs()

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.REJECTED, repository.syncState.value)
    }

    // =========================================================================
    // Mecanismo de reintento: retrySync()
    // =========================================================================

    @Test
    fun retrySync_cuandoOffline_establecePendingSyncYRetornaNetworkUnavailableException() = runTest {
        isOnline = false

        val result = repository.retrySync()

        assertTrue(result is KronoResult.Error)
        assertTrue((result as KronoResult.Error).throwable is NetworkUnavailableException)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun retrySync_desdePendingSync_cuandoOnlineYRemotoExitoso_transicionaAConfirmed() = runTest {
        // Provocamos estado PENDING_SYNC inicial
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()
        repository.updateProfile("Nombre Pendiente", "Estudiante")
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)

        // Se restablece la conexión
        isOnline = true
        remoteDataSource.shouldFail = null

        val result = repository.retrySync()

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
        assertEquals("Nombre Pendiente", remoteDataSource.lastSavedName)
    }

    @Test
    fun retrySync_desdeRejected_cuandoOnlineYRemotoExitoso_transicionaAConfirmed() = runTest {
        // Provocamos estado REJECTED inicial
        remoteDataSource.shouldFail = IllegalStateException("Error temporal")
        repository.updateProfile("Nombre Rechazado", "Estudiante")
        assertEquals(SyncState.REJECTED, repository.syncState.value)

        // Se corrige el error remoto y se reintenta
        remoteDataSource.shouldFail = null

        val result = repository.retrySync()

        assertTrue(result is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
        assertEquals("Nombre Rechazado", remoteDataSource.lastSavedName)
    }

    @Test
    fun retrySync_cuandoOnlinePeroRemotoFalla_estableceRejectedYRetornaError() = runTest {
        // Provocamos estado PENDING_SYNC inicial
        isOnline = false
        remoteDataSource.shouldFail = NetworkUnavailableException()
        repository.updateProfile("Nombre Intento", "Estudiante")
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)

        // Se recupera la conectividad pero Firestore rechaza
        isOnline = true
        remoteDataSource.shouldFail = IllegalStateException("PERMISSION_DENIED_EN_RETRY")

        val result = repository.retrySync()

        assertTrue(result is KronoResult.Error)
        assertEquals(SyncState.REJECTED, repository.syncState.value)
    }

    @Test
    fun retrySync_cuandoOnlinePeroRemotoLanzaNetworkUnavailable_establecePendingSync() = runTest {
        // En caso de que falle con NetworkUnavailableException durante el retry
        isOnline = true
        remoteDataSource.shouldFail = NetworkUnavailableException()

        val result = repository.retrySync()

        assertTrue(result is KronoResult.Error)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)
    }

    @Test
    fun resetUserData_purgaTareasEnTaskRepository() = runTest {
        val fakeTaskRepo = com.krono.core.domain.task.FakeTaskRepository()
        val repoWithTask = ProfileRepositoryImpl(
            localStore = localStore,
            remoteDataSource = remoteDataSource,
            connectivityChecker = ConnectivityChecker { isOnline },
            taskRepository = fakeTaskRepo,
        )

        val result = repoWithTask.resetUserData()

        assertTrue(result is KronoResult.Success)
        assertEquals(1, fakeTaskRepo.clearTasksForUserCalls)
        assertEquals("user_test", fakeTaskRepo.lastClearedUserId)
    }

    @Test
    fun deleteAccount_purgaTareasEnTaskRepository() = runTest {
        val fakeTaskRepo = com.krono.core.domain.task.FakeTaskRepository()
        val repoWithTask = ProfileRepositoryImpl(
            localStore = localStore,
            remoteDataSource = remoteDataSource,
            connectivityChecker = ConnectivityChecker { isOnline },
            taskRepository = fakeTaskRepo,
        )

        val result = repoWithTask.deleteAccount("password123")

        assertTrue(result is KronoResult.Success)
        assertEquals(1, fakeTaskRepo.clearTasksForUserCalls)
        assertEquals("user_test", fakeTaskRepo.lastClearedUserId)
    }
}
