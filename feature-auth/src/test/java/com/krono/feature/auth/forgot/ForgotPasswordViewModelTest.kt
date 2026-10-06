package com.krono.feature.auth.forgot

import androidx.lifecycle.SavedStateHandle
import com.krono.core.domain.auth.FakeAuthRepository
import com.krono.core.domain.auth.ResetPasswordUseCase
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository().apply {
        givenRegisteredUser("ana@correo.com", "krono2026!")
    }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        ForgotPasswordViewModel(savedState, ResetPasswordUseCase(repository))

    @Test
    fun elEstadoInicialEstaVacio() {
        val estado = viewModel().uiState.value

        assertEquals("", estado.email)
        assertFalse(estado.isLoading)
        assertFalse(estado.isSent)
        assertNull(estado.error)
    }

    @Test
    fun conCorreoVacioMuestraCamposObligatorios() = runTest {
        val vm = viewModel()

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.EmptyFields, vm.uiState.value.error)
    }

    @Test
    fun conCorreoNoRegistradoMuestraQueNoEstaRegistradoEnKrono() = runTest {
        val vm = viewModel()
        vm.onEmailChange("nadie@correo.com")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.EmailNotRegistered, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSent)
    }

    @Test
    fun conCorreoRegistradoPasaALaPantallaDeExito() = runTest {
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")

        vm.onSubmit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSent)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun duranteElEnvioMuestraProgresoYNoPermiteDobleEnvio() = runTest {
        repository.gate = CompletableDeferred()
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")

        vm.onSubmit()
        vm.onSubmit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)
        assertEquals(1, repository.resetCalls)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun unaFallaInesperadaMuestraUnMensajeGenerico() = runTest {
        repository.failure = RuntimeException("io")
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.Unexpected, vm.uiState.value.error)
    }

    @Test
    fun editarElCorreoBorraElError() = runTest {
        val vm = viewModel()
        vm.onSubmit()
        advanceUntilIdle()

        vm.onEmailChange("a")

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun elCorreoYElExitoSobrevivenALaRecreacionDelProceso() = runTest {
        val savedState = SavedStateHandle()
        viewModel(savedState).apply {
            onEmailChange("ana@correo.com")
            onSubmit()
        }
        advanceUntilIdle()

        val recreado = viewModel(savedState).uiState.value

        assertEquals("ana@correo.com", recreado.email)
        assertTrue(recreado.isSent)
    }
}
