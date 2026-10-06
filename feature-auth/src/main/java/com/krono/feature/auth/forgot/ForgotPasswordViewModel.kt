package com.krono.feature.auth.forgot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.ResetPasswordUseCase
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
 * Estado de "Recuperar contraseña" (simulado, sin envío real de correo).
 * Inicial: correo vacío; cargando: [isLoading]; error: [error]; éxito: [isSent].
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AuthMessage? = null,
    val isSent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val resetPassword: ResetPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ForgotPasswordUiState(
            email = savedStateHandle[KEY_EMAIL] ?: "",
            isSent = savedStateHandle[KEY_SENT] ?: false,
        ),
    )
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isLoading) return // sin doble envío
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = resetPassword(current.email)) {
                is KronoResult.Success -> {
                    savedStateHandle[KEY_SENT] = true
                    _uiState.update { it.copy(isLoading = false, isSent = true) }
                }
                is KronoResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.throwable.toAuthMessage())
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "forgot_email"
        const val KEY_SENT = "forgot_sent"
    }
}
