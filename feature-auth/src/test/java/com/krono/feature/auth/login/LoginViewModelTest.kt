package com.krono.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.FakeAuthRepository
import com.krono.core.domain.auth.LoginUseCase
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

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository().apply {
        givenRegisteredUser("ana@correo.com", "krono2026!")
    }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(savedState, LoginUseCase(repository))

    @Test
    fun elEstadoInicialEstaVacioYSinErrores() {
        val estado = viewModel().uiState.value

        assertEquals("", estado.email)
        assertEquals("", estado.password)
        assertFalse(estado.isLoading)
        assertFalse(estado.isPasswordVisible)
        assertNull(estado.error)
    }

    @Test
    fun conCamposVaciosMuestraElMensajeDeCamposObligatorios() = runTest {
        val vm = viewModel()

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.EmptyFields, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun duranteElEnvioMuestraProgresoYNoPermiteDobleEnvio() = runTest {
        repository.gate = CompletableDeferred()
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("krono2026!")

        vm.onSubmit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoading)

        vm.onSubmit()
        advanceUntilIdle()
        assertEquals(1, repository.loginCalls)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun conCredencialesCorrectasEmiteUnEventoDeNavegacion() = runTest {
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("krono2026!")

        vm.events.test {
            vm.onSubmit()
            assertEquals(LoginEvent.LoggedIn, awaitItem())
        }
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun conCredencialesIncorrectasMuestraElErrorYConservaLoEscrito() = runTest {
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("otra2026!")

        vm.onSubmit()
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(AuthMessage.InvalidCredentials, estado.error)
        assertEquals("ana@correo.com", estado.email)
        assertEquals("otra2026!", estado.password)
        assertFalse(estado.isLoading)
    }

    @Test
    fun conCorreoMalFormadoMuestraCorreoInvalido() = runTest {
        val vm = viewModel()
        vm.onEmailChange("ana@correo")
        vm.onPasswordChange("krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.InvalidEmail, vm.uiState.value.error)
    }

    @Test
    fun sinConexionMuestraElMensajeDeSinConexion() = runTest {
        repository.failure = AuthError.NoConnection
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.NoConnection, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun conDemasiadosIntentosMuestraElMensajeDeEspera() = runTest {
        repository.failure = AuthError.TooManyRequests
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.TooManyRequests, vm.uiState.value.error)
    }

    @Test
    fun unaFallaInesperadaMuestraUnMensajeGenericoNoTecnico() = runTest {
        repository.failure = IllegalStateException("INTERNAL_ERROR")
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.Unexpected, vm.uiState.value.error)
    }

    @Test
    fun editarUnCampoBorraElErrorAnterior() = runTest {
        val vm = viewModel()
        vm.onSubmit()
        advanceUntilIdle()

        vm.onEmailChange("a")

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun alternaLaVisibilidadDeLaContrasena() {
        val vm = viewModel()

        vm.onTogglePasswordVisibility()
        assertTrue(vm.uiState.value.isPasswordVisible)

        vm.onTogglePasswordVisibility()
        assertFalse(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun elCorreoSobreviveALaRecreacionDelProcesoPeroLaContrasenaNoSeGuarda() {
        val savedState = SavedStateHandle()
        viewModel(savedState).apply {
            onEmailChange("ana@correo.com")
            onPasswordChange("krono2026!")
        }

        val recreado = viewModel(savedState).uiState.value

        assertEquals("ana@correo.com", recreado.email)
        assertEquals("", recreado.password)
    }
}
