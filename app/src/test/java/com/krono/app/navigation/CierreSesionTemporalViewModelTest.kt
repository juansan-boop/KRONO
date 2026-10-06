package com.krono.app.navigation

import app.cash.turbine.test
import com.krono.core.domain.auth.FakeAuthRepository
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.core.domain.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// TEMPORAL: se elimina cuando existan F-01/F-19 (feature-profile)
@OptIn(ExperimentalCoroutinesApi::class)
class CierreSesionTemporalViewModelTest {

    private val repository = FakeAuthRepository().also {
        it.givenActiveSession(User(id = "1", email = "ana@correo.com"))
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = CierreSesionTemporalViewModel(LogoutUseCase(repository))

    @Test
    fun alCerrarSesionSeLimpiaLaSesionYSeEmiteLaNavegacionAlLogin() = runTest {
        val vm = viewModel()

        vm.sesionCerrada.test {
            vm.cerrarSesion()
            advanceUntilIdle()

            assertEquals(Unit, awaitItem())
            assertNull(repository.currentSession)
        }
    }

    @Test
    fun siElCierreFallaNoSeNavegaYSeMuestraElError() = runTest {
        repository.failure = IllegalStateException("fallo")
        val vm = viewModel()

        vm.sesionCerrada.test {
            vm.cerrarSesion()
            advanceUntilIdle()

            expectNoEvents()
            assertTrue(vm.hayError.value)
            assertFalse(vm.cerrando.value)
        }
    }

    @Test
    fun trasCerrarSesionAlReabrirLaAppArrancaEnElLogin() = runTest {
        val vm = viewModel()
        vm.cerrarSesion()
        advanceUntilIdle()

        val reabierta = KronoAppViewModel(ObserveSessionUseCase(repository))
        advanceUntilIdle()

        assertEquals(StartDestination.Auth, reabierta.startDestination.value)
    }
}
