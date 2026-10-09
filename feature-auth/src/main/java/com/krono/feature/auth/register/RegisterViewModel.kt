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
    /** Correo escrito por el usuario. */
    val email: String = "",
    /** Contraseña escrita; solo vive en memoria. */
    val password: String = "",
    /** Confirmación de la contraseña. */
    val confirmation: String = "",
    /** La contraseña se muestra en claro. */
    val isPasswordVisible: Boolean = false,
    /** La confirmación se muestra en claro. */
    val isConfirmationVisible: Boolean = false,
    /** El usuario ya intentó enviar el formulario. */
    val submitAttempted: Boolean = false,
    /** Hay un registro en curso. */
    val isLoading: Boolean = false,
    /** Mensaje de error vigente, o `null`. */
    val error: AuthMessage? = null,
    /** La cuenta se creó; la UI muestra "¡Cuenta creada!". */
    val isRegistered: Boolean = false,
) {
    /** Indicadores en vivo de la política de contraseña. */
    val requirements: PasswordRequirements get() = PasswordPolicy.evaluate(password)

    /** "Las contraseñas no coinciden": visible tras el primer intento y se actualiza en vivo. */
    val showPasswordsMismatch: Boolean
        get() = submitAttempted && confirmation.isNotEmpty() && confirmation != password
}

/**
 * Lógica de "Crear cuenta" (F-23): valida mediante [RegisterUseCase] y publica el resultado en [RegisterUiState].
 */
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
    /** Estado que observa la pantalla. */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** Actualiza el correo y borra el error anterior. */
    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    /** Actualiza la contraseña; los indicadores de requisitos se recalculan en vivo. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    /** Actualiza la confirmación de la contraseña. */
    fun onConfirmationChange(value: String) {
        _uiState.update { it.copy(confirmation = value, error = null) }
    }

    /** Alterna entre mostrar y ocultar la contraseña. */
    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /** Alterna entre mostrar y ocultar la confirmación. */
    fun onToggleConfirmationVisibility() {
        _uiState.update { it.copy(isConfirmationVisible = !it.isConfirmationVisible) }
    }

    /** Crea la cuenta con los datos actuales; ignora el envío si ya hay uno en curso. */
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
        /** Clave del correo en el [SavedStateHandle]. */
        const val KEY_EMAIL = "register_email"
        /** Clave del indicador de cuenta creada en el [SavedStateHandle]. */
        const val KEY_REGISTERED = "register_done"
    }
}
