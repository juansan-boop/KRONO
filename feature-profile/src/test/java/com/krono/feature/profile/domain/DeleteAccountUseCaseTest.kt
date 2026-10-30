package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [DeleteAccountUseCase]: conectividad, orden seguro de borrado y errores (F-21 CA-2). */
class DeleteAccountUseCaseTest {

    private val log = mutableListOf<String>()
    private val remote = NoOpProfileRemoteDataSource(log)
    private var online = true
    private val store = ProfileLocalStore().apply {
        bindUser(USER_ID, "user@krono.app")
        saveProfile(MODIFIED_NAME, MODIFIED_ROLE)
    }

    private fun useCase() = DeleteAccountUseCase(store, remote, ConnectivityChecker { online })

    @Test
    fun sinRedFallaConNetworkUnavailableSinReautenticarNiBorrar() = runTest {
        online = false

        val result = useCase()(PASSWORD)

        assertTrue((result as KronoResult.Error).throwable is NetworkUnavailableException)
        assertTrue(log.isEmpty())
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun reautenticacionFallidaNoBorraNadaYPropagaElError() = runTest {
        remote.reauthenticateFailure = InvalidPasswordException()

        val result = useCase()(PASSWORD)

        assertTrue((result as KronoResult.Error).throwable is InvalidPasswordException)
        assertEquals(listOf(NoOpProfileRemoteDataSource.CALL_REAUTHENTICATE), log)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun exitoReautenticaLuegoBorraDatosYCuentaEnUnaSolaOperacionYLimpiaElAlmacenLocal() = runTest {
        val result = useCase()(PASSWORD)

        assertTrue(result is KronoResult.Success)
        assertEquals(
            listOf(
                NoOpProfileRemoteDataSource.CALL_REAUTHENTICATE,
                NoOpProfileRemoteDataSource.CALL_DELETE_ACCOUNT,
            ),
            log,
        )
        assertEquals(USER_ID, remote.lastDeleteUserId)
        assertEquals(ProfileLocalStore.DEFAULT_ROLE, store.userRole.value)
    }

    @Test
    fun tiempoLimiteAlEliminarDevuelveErrorYConservaElAlmacenLocal() = runTest {
        remote.shouldFail = RemoteTimeoutException()

        val result = useCase()(PASSWORD)

        assertTrue((result as KronoResult.Error).throwable is RemoteTimeoutException)
        assertEquals(MODIFIED_NAME, store.userName.value)
    }

    @Test
    fun contrasenaEnBlancoDevuelveErrorSinTocarNada() = runTest {
        val result = useCase()("  ")

        assertTrue((result as KronoResult.Error).throwable is BlankPasswordException)
        assertTrue(log.isEmpty())
    }

    private companion object {
        const val USER_ID = "user_abc"
        const val PASSWORD = "Password123*"
        const val MODIFIED_NAME = "Nombre Modificado"
        const val MODIFIED_ROLE = "Rol Modificado"
    }
}
