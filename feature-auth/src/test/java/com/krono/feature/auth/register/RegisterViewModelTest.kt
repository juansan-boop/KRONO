package com.krono.feature.auth.register

import androidx.lifecycle.SavedStateHandle
import com.krono.core.domain.auth.FakeAuthRepository
import com.krono.core.domain.auth.PasswordRequirements
import com.krono.core.domain.auth.RegisterUseCase
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

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        RegisterViewModel(savedState, RegisterUseCase(repository))

    private fun RegisterViewModel.fill(email: String, password: String, confirmation: String) {
        onEmailChange(email)
        onPasswordChange(password)
        onConfirmationChange(confirmation)
    }

    @Test
    fun elEstadoInicialNoCumpleRequisitosNiMuestraErrores() {
        val estado = viewModel().uiState.value

        assertEquals(PasswordRequirements(minLength = false, hasDigit = false, hasSpecialChar = false), estado.requirements)
        assertFalse(estado.submitAttempted)
        assertFalse(estado.showPasswordsMismatch)
        assertFalse(estado.isRegistered)
        assertNull(estado.error)
    }

    @Test
    fun losIndicadoresDeContrasenaSeActualizanEnVivo() {
        val vm = viewModel()

        vm.onPasswordChange("abcdefgh")
        assertEquals(PasswordRequirements(minLength = true, hasDigit = false, hasSpecialChar = false), vm.uiState.value.requirements)

        vm.onPasswordChange("abcdefg1!")
        assertTrue(vm.uiState.value.requirements.isSatisfied)
    }

    @Test
    fun conCamposVaciosMuestraElMensajeDeCamposObligatorios() = runTest {
        val vm = viewModel()

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.EmptyFields, vm.uiState.value.error)
        assertTrue(vm.uiState.value.submitAttempted)
    }

    @Test
    fun conCorreoMalFormadoMuestraCorreoInvalido() = runTest {
        val vm = viewModel()
        vm.fill("anacorreo.com", "krono2026!", "krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.InvalidEmail, vm.uiState.value.error)
    }

    @Test
    fun conContrasenaDebilMarcaLosRequisitosIncumplidos() = runTest {
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono", "krono")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.WeakPassword, vm.uiState.value.error)
        assertTrue(vm.uiState.value.submitAttempted)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun conConfirmacionDistintaMuestraQueLasContrasenasNoCoinciden() = runTest {
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono2026!", "krono2026?")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.PasswordsDoNotMatch, vm.uiState.value.error)
        assertTrue(vm.uiState.value.showPasswordsMismatch)
    }

    @Test
    fun trasUnIntentoElAvisoDeNoCoincidenciaSeActualizaEnVivo() = runTest {
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono2026!", "krono2026?")
        vm.onSubmit()
        advanceUntilIdle()

        vm.onConfirmationChange("krono2026!")

        assertFalse(vm.uiState.value.showPasswordsMismatch)
    }

    @Test
    fun conCorreoYaRegistradoMuestraElError() = runTest {
        repository.givenRegisteredUser("ana@correo.com", "krono2026!")
        val vm = viewModel()
        vm.fill("ana@correo.com", "nueva2026!", "nueva2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.EmailAlreadyRegistered, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isRegistered)
    }

    @Test
    fun unaFallaInesperadaMuestraUnMensajeGenerico() = runTest {
        repository.failure = RuntimeException("disk I/O error")
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono2026!", "krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthMessage.Unexpected, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun duranteElEnvioMuestraProgresoYNoPermiteDobleEnvio() = runTest {
        repository.gate = CompletableDeferred()
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono2026!", "krono2026!")

        vm.onSubmit()
        vm.onSubmit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)
        assertEquals(1, repository.registerCalls)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun conDatosValidosPasaAlEstadoDeCuentaCreada() = runTest {
        val vm = viewModel()
        vm.fill("ana@correo.com", "krono2026!", "krono2026!")

        vm.onSubmit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isRegistered)
        assertNull(vm.uiState.value.error)
        assertEquals("ana@correo.com", repository.currentSession?.email)
    }

    @Test
    fun alternaLaVisibilidadDeCadaContrasenaPorSeparado() {
        val vm = viewModel()

        vm.onTogglePasswordVisibility()
        assertTrue(vm.uiState.value.isPasswordVisible)
        assertFalse(vm.uiState.value.isConfirmationVisible)

        vm.onToggleConfirmationVisibility()
        assertTrue(vm.uiState.value.isConfirmationVisible)
    }

    @Test
    fun elCorreoYElExitoSobrevivenALaRecreacionDelProceso() = runTest {
        val savedState = SavedStateHandle()
        viewModel(savedState).apply {
            fill("ana@correo.com", "krono2026!", "krono2026!")
            onSubmit()
        }
        advanceUntilIdle()

        val recreado = viewModel(savedState).uiState.value

        assertEquals("ana@correo.com", recreado.email)
        assertTrue(recreado.isRegistered)
        assertEquals("", recreado.password)
    }
}
