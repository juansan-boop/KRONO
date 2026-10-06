package com.krono.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.LoginUseCase
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.toAuthMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de "Iniciar sesión". Inicial: campos vacíos; cargando: [isLoading];
 * error: [error]; éxito: se emite [LoginEvent.LoggedIn].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthMessage? = null,
)

/** Eventos de un solo uso, separados del estado para que no se repitan al rotar. */
sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val login: LoginUseCase,
) : ViewModel() {

    // El correo se guarda en SavedStateHandle (sobrevive a la muerte del proceso).
    // La contraseña solo vive en memoria del ViewModel, que ya sobrevive a la rotación:
    // no se escribe en el Bundle del sistema.
    private val _uiState = MutableStateFlow(LoginUiState(email = savedStateHandle[KEY_EMAIL] ?: ""))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return // sin doble envío
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = login(current.email, current.password)) {
                is KronoResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.LoggedIn)
                }
                is KronoResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.throwable.toAuthMessage())
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "login_email"
    }
}
