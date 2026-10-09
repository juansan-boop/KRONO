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
 * Estado de "Recuperar contraseña" (envío real con Firebase).
 * Inicial: correo vacío; cargando: [isLoading]; error: [error]; éxito: [isSent].
 */
data class ForgotPasswordUiState(
    /** Correo escrito por el usuario. */
    val email: String = "",
    /** Hay un envío en curso. */
    val isLoading: Boolean = false,
    /** Mensaje de error vigente, o `null`. */
    val error: AuthMessage? = null,
    /** El enlace de recuperación ya se solicitó. */
    val isSent: Boolean = false,
)

/**
 * Lógica de "Recuperar contraseña" (F-24). Guarda el correo y el éxito en
 * [SavedStateHandle] para que sobrevivan a la muerte del proceso.
 */
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
    /** Estado que observa la pantalla. */
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** Actualiza el correo y borra el error anterior. */
    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, error = null) }
    }

    /** Pide el enlace de recuperación; ignora el envío si ya hay uno en curso. */
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
        /** Clave del correo en el [SavedStateHandle]. */
        const val KEY_EMAIL = "forgot_email"
        /** Clave del indicador de envío exitoso en el [SavedStateHandle]. */
        const val KEY_SENT = "forgot_sent"
    }
}
