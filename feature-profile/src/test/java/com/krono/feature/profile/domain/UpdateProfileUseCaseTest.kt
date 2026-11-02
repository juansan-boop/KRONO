package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [UpdateProfileUseCase]: errores de validación tipados y persistencia local primero (F-19 CA-2). */
class UpdateProfileUseCaseTest {

    private val store = ProfileLocalStore()
    private val remote = NoOpProfileRemoteDataSource()
    private val useCase = UpdateProfileUseCase(store, remote)

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
    fun datosValidosSePersistenLocalYRemotamente() = runTest {
        val result = useCase("Ana Torres", "Estudiante")

        assertTrue(result is KronoResult.Success)
        assertEquals("Ana Torres", store.userName.value)
        assertEquals("Ana Torres", remote.lastSavedName)
    }

    @Test
    fun rechazoRemotoComoResultadoDeErrorConservaElCambioLocalYDevuelveExito() = runTest {
        remote.shouldFail = IllegalStateException("PERMISSION_DENIED")

        val result = useCase("Ana Torres", "Estudiante")

        assertTrue(result is KronoResult.Success)
        assertEquals("Ana Torres", store.userName.value)
    }
}
