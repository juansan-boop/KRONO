package com.krono.feature.profile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.DeleteAccountUseCase
import com.krono.feature.profile.domain.ResetUserDataUseCase
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
 * ViewModel que gobierna la Zona de Seguridad y purga de datos (F-21 CA-1, CA-2).
 *
 * Administra el estado [SecurityZoneUiState], el flujo de confirmación en dos pasos
 * tanto para el restablecimiento de datos (CA-1) como para la eliminación destructiva
 * con reautenticación sensible en Firebase (CA-2).
 *
 * Sigue estrictamente la regla arquitectónica de KRONO: no importa `androidx.compose.*`.
 *
 * @property resetUserData Caso de uso para restablecer datos y configuración (CA-1).
 * @property deleteAccount Caso de uso para reautenticar y eliminar la cuenta (CA-2).
 */
@HiltViewModel
class SecurityZoneViewModel @Inject constructor(
    private val resetUserData: ResetUserDataUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityZoneUiState())

    /** Flujo de estado visual inmutable observable por la UI. */
    val uiState: StateFlow<SecurityZoneUiState> = _uiState.asStateFlow()

    private val _events = Channel<SecurityZoneEvent>(Channel.BUFFERED)

    /** Canal de eventos de un solo uso para navegación desacoplada. */
    val events: Flow<SecurityZoneEvent> = _events.receiveAsFlow()

    /**
     * Selecciona la acción que el usuario desea ejecutar en el paso 1.
     *
     * @param action Tipo de acción elegida ([SecurityActionType.RESET_DATA] o [SecurityActionType.DELETE_ACCOUNT]).
     */
    fun onSelectAction(action: SecurityActionType) {
        if (_uiState.value.isOperationInProgress) return
        _uiState.update { it.copy(selectedAction = action, errorMessage = null) }
    }

    /**
     * Avanza al segundo paso de confirmación abriendo el diálogo correspondiente a la acción seleccionada.
     */
    fun onContinueToConfirmation() {
        if (_uiState.value.isOperationInProgress) return

        when (_uiState.value.selectedAction) {
            SecurityActionType.RESET_DATA -> {
                _uiState.update {
                    it.copy(
                        showResetConfirmationDialog = true,
                        errorMessage = null,
                    )
                }
            }
            SecurityActionType.DELETE_ACCOUNT -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = true,
                        reauthPassword = "",
                        reauthPasswordError = null,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Cierra el diálogo de restablecimiento de datos del paso 2.
     * Si la operación está en curso ([SecurityZoneUiState.isResettingData]), la petición se ignora por seguridad.
     */
    fun onDismissResetDialog() {
        if (_uiState.value.isResettingData) return
        _uiState.update { it.copy(showResetConfirmationDialog = false, errorMessage = null) }
    }

    /**
     * Confirma la ejecución del restablecimiento de datos y configuración en el paso 2 (CA-1).
     *
     * Bloquea envíos concurrentes mientras [SecurityZoneUiState.isResettingData] sea true.
     */
    fun onConfirmResetData() {
        val currentState = _uiState.value
        if (currentState.isResettingData) return

        _uiState.update {
            it.copy(
                isResettingData = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = resetUserData()) {
                is KronoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isResettingData = false,
                            showResetConfirmationDialog = false,
                            successMessage = "Configuración y datos restablecidos correctamente",
                        )
                    }
                    _events.send(SecurityZoneEvent.DataResetSuccess)
                }
                is KronoResult.Error -> {
                    val message = result.throwable.message ?: "No fue posible restablecer los datos. Intenta de nuevo."
                    _uiState.update {
                        it.copy(
                            isResettingData = false,
                            errorMessage = message,
                        )
                    }
                }
                is KronoResult.Loading -> {
                    _uiState.update { it.copy(isResettingData = true) }
                }
            }
        }
    }

    /**
     * Cierra el diálogo de eliminación de cuenta del paso 2.
     * Si la operación está en curso ([SecurityZoneUiState.isDeletingAccount]), la petición se ignora por seguridad.
     */
    fun onDismissDeleteDialog() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update {
            it.copy(
                showDeleteAccountDialog = false,
                reauthPassword = "",
                reauthPasswordError = null,
                errorMessage = null,
            )
        }
    }

    /**
     * Actualiza el valor del campo de contraseña para la reautenticación sensible en Firebase.
     *
     * @param password Contraseña ingresada por el usuario.
     */
    fun onReauthPasswordChange(password: String) {
        _uiState.update { current ->
            current.copy(
                reauthPassword = password,
                reauthPasswordError = if (password.isNotBlank()) null else current.reauthPasswordError,
                errorMessage = null,
            )
        }
    }

    /**
     * Alterna la visibilidad en texto plano de la contraseña en el campo de reautenticación.
     */
    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * Confirma la eliminación definitiva de la cuenta en el paso 2 con reautenticación (CA-2).
     *
     * Valida que la contraseña no esté vacía antes de invocar la persistencia y bloquea envíos concurrentes.
     */
    fun onConfirmDeleteAccount() {
        val currentState = _uiState.value
        if (currentState.isDeletingAccount) return

        if (currentState.reauthPassword.isBlank()) {
            _uiState.update { it.copy(reauthPasswordError = "Ingresa tu contraseña actual para confirmar") }
            return
        }

        _uiState.update {
            it.copy(
                isDeletingAccount = true,
                reauthPasswordError = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = deleteAccount(currentState.reauthPassword)) {
                is KronoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            showDeleteAccountDialog = false,
                        )
                    }
                    _events.send(SecurityZoneEvent.AccountDeleted)
                }
                is KronoResult.Error -> {
                    val message = result.throwable.message ?: "No fue posible eliminar la cuenta. Verifica tu contraseña."
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            errorMessage = message,
                        )
                    }
                }
                is KronoResult.Loading -> {
                    _uiState.update { it.copy(isDeletingAccount = true) }
                }
            }
        }
    }

    /**
     * Notifica el retorno a la pantalla previa (Hub de Ajustes).
     */
    fun onBackClick() {
        if (_uiState.value.isOperationInProgress) return
        viewModelScope.launch {
            _events.send(SecurityZoneEvent.NavigateBack)
        }
    }
}
