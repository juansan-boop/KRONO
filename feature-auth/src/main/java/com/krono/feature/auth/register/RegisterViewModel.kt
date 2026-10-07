package com.krono.feature.auth.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.PasswordPolicy
import com.krono.core.domain.auth.PasswordRequirements
import com.krono.core.domain.auth.RegisterUseCase
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.toAuthMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de "Crear cuenta". Inicial: campos vacíos; cargando: [isLoading];
 * error: [error] y, tras un intento ([submitAttempted]), requisitos incumplidos
 * y [showPasswordsMismatch]; éxito: [isRegistered] ("¡Cuenta creada!").
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmationVisible: Boolean = false,
    val submitAttempted: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthMessage? = null,
    val isRegistered: Boolean = false,
) {
    /** Indicadores en vivo de la política de contraseña. */
    val requirements: PasswordRequirements get() = PasswordPolicy.evaluate(password)

    /** "Las contraseñas no coinciden": visible tras el primer intento y se actualiza en vivo. */
    val showPasswordsMismatch: Boolean
        get() = submitAttempted && confirmation.isNotEmpty() && confirmation != password
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val register: RegisterUseCase,
) : ViewModel() {

    // Solo correo y éxito van al SavedStateHandle; las contraseñas quedan en memoria.
    private val _uiState = MutableStateFlow(
        RegisterUiState(
            email = savedStateHandle[KEY_EMAIL] ?: "",
            isRegistered = savedStateHandle[KEY_REGISTERED] ?: false,
        ),
    )
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onConfirmationChange(value: String) {
        _uiState.update { it.copy(confirmation = value, error = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmationVisibility() {
        _uiState.update { it.copy(isConfirmationVisible = !it.isConfirmationVisible) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading || current.isRegistered) return // sin doble envío
        _uiState.update { it.copy(isLoading = true, submitAttempted = true, error = null) }
        viewModelScope.launch {
            when (val result = register(current.email, current.password, current.confirmation)) {
                is KronoResult.Success -> {
                    savedStateHandle[KEY_REGISTERED] = true
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                }
                is KronoResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.throwable.toAuthMessage())
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "register_email"
        const val KEY_REGISTERED = "register_done"
    }
}
