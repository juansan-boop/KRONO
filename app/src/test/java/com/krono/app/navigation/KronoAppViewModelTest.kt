package com.krono.app.navigation

import com.krono.core.domain.auth.FakeAuthRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KronoAppViewModelTest {

    private val repository = FakeAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = KronoAppViewModel(ObserveSessionUseCase(repository))

    @Test
    fun mientrasSeLeeLaSesionElDestinoEstaCargando() {
        assertEquals(StartDestination.Loading, viewModel().startDestination.value)
    }

    @Test
    fun sinSesionActivaArrancaEnAutenticacion() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        assertEquals(StartDestination.Auth, vm.startDestination.value)
    }

    @Test
    fun conSesionActivaEntraDirectoSinPasarPorElLogin() = runTest {
        repository.givenActiveSession(User(id = "1", email = "ana@correo.com"))
        val vm = viewModel()

        advanceUntilIdle()

        assertEquals(StartDestination.Home, vm.startDestination.value)
    }

    @Test
    fun elDestinoInicialNoCambiaSiLaSesionCambiaDespues() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // El registro inicia sesión, pero la app debe seguir mostrando "¡Cuenta creada!":
        // la navegación posterior la deciden los callbacks, no este valor.
        repository.givenActiveSession(User(id = "1", email = "ana@correo.com"))
        advanceUntilIdle()

        assertEquals(StartDestination.Auth, vm.startDestination.value)
    }
}
