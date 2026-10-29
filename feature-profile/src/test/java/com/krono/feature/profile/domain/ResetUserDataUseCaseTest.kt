package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [ResetUserDataUseCase]: conectividad previa, atomicidad y propagación de errores (F-21 CA-1). */
class ResetUserDataUseCaseTest {

    private val log = mutableListOf<String>()
    private val remote = NoOpProfileRemoteDataSource(log)
    private var online = true
    private val store = ProfileLocalStore().apply {
        bindUser(USER_ID, "user@krono.app")
        saveProfile(MODIFIED_NAME, MODIFIED_ROLE)
    }

    private fun useCase() = ResetUserDataUseCase(store, remote, ConnectivityChecker { online })

    @Test
    fun sinRedFallaRapidoConNetworkUnavailableSinEmitirNingunaEscritura() = runTest {
        online = false

        val result = useCase()()

        assertTrue(result is KronoResult.Error)
        assertTrue((result as KronoResult.Error).throwable is NetworkUnavailableException)
        assertTrue("No debe emitirse ninguna operación remota", log.isEmpty())
        assertEquals(MODIFIED_NAME, store.userName.value)
        assertEquals(MODIFIED_ROLE, store.userRole.value)
    }

    @Test
    fun errorRemotoDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        remote.shouldFail = IllegalStateException("PERMISSION_DENIED")

        val result = useCase()()

        assertTrue(result is KronoResult.Error)
        assertEquals(MODIFIED_NAME, store.userName.value)
        assertEquals(MODIFIED_ROLE, store.userRole.value)
    }

    @Test
    fun tiempoLimiteAgotadoDevuelveErrorYDejaIntactoElAlmacenLocal() = runTest {
        remote.shouldFail = RemoteTimeoutException()

        val result = useCase()()

        assertTrue((result as KronoResult.Error).throwable is RemoteTimeoutException)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun exitoEmiteUnaSolaOperacionRemotaAtomicaYLimpiaElAlmacenLocal() = runTest {
        val result = useCase()()

        assertTrue(result is KronoResult.Success)
        assertEquals(listOf(NoOpProfileRemoteDataSource.CALL_RESET_USER_DATA), log)
        assertEquals(USER_ID, remote.lastResetUserId)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
    }

    private companion object {
        const val USER_ID = "user_abc"
        const val MODIFIED_NAME = "Nombre Modificado"
        const val MODIFIED_ROLE = "Rol Modificado"
    }
}
