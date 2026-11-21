package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [UpdateProfileUseCase]: errores de validación tipados y persistencia local primero (F-19 CA-2). */
class UpdateProfileUseCaseTest {

    private val store = ProfileLocalStore()
    private val remote = NoOpProfileRemoteDataSource()
    private var isOnline = true
    private val repository = ProfileRepositoryImpl(store, remote, ConnectivityChecker { isOnline })
    private val useCase = UpdateProfileUseCase(repository)

    @Test
    fun nombreEnBlancoDevuelveBlankNameExceptionSinPersistir() = runTest {
        val result = useCase("   ", "Estudiante")

        assertTrue((result as KronoResult.Error).throwable is BlankNameException)
        assertNull(remote.lastSavedName)
    }

    @Test
    fun rolEnBlancoDevuelveBlankRoleExceptionSinPersistir() = runTest {
        val result = useCase("Ana Torres", "  ")

        assertTrue((result as KronoResult.Error).throwable is BlankRoleException)
        assertNull(remote.lastSavedName)
    }

    @Test
    fun datosValidosSePersistenLocalYRemotamenteConEstadoConfirmed() = runTest {
        val result = useCase("Ana Torres", "Estudiante")

        assertTrue(result is KronoResult.Success)
        assertEquals("Ana Torres", store.userName.value)
        assertEquals("Ana Torres", remote.lastSavedName)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }

    @Test
    fun actualizacionSinConexionDejaEstadoEnPendingSyncYReintentoExitosoTransicionaAConfirmed() = runTest {
        isOnline = false
        remote.shouldFail = NetworkUnavailableException()

        val result = useCase("Ana Torres", "Estudiante")

        assertTrue("Debe retornar éxito local aunque esté offline", result is KronoResult.Success)
        assertEquals("Ana Torres", store.userName.value)
        assertEquals(SyncState.PENDING_SYNC, repository.syncState.value)

        // Se restaura conectividad y se reintenta
        isOnline = true
        remote.shouldFail = null

        val retryResult = repository.retrySync()
        assertTrue(retryResult is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
        assertEquals("Ana Torres", remote.lastSavedName)
    }

    @Test
    fun rechazoRemotoComoResultadoDeErrorConservaElCambioLocalYDejaEstadoEnRejected() = runTest {
        remote.shouldFail = IllegalStateException("PERMISSION_DENIED")

        val result = useCase("Ana Torres", "Estudiante")

        assertTrue(result is KronoResult.Success)
        assertEquals("Ana Torres", store.userName.value)
        assertEquals(SyncState.REJECTED, repository.syncState.value)

        // Reintento exitoso tras resolver el problema
        remote.shouldFail = null
        val retryResult = repository.retrySync()
        assertTrue(retryResult is KronoResult.Success)
        assertEquals(SyncState.CONFIRMED, repository.syncState.value)
    }
}
