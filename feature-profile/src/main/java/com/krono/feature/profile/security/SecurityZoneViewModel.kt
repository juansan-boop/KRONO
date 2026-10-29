package com.krono.feature.profile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.DeleteAccountUseCase
import com.krono.feature.profile.domain.InvalidPasswordException
import com.krono.feature.profile.domain.NetworkUnavailableException
import com.krono.feature.profile.domain.RemoteTimeoutException
import com.krono.feature.profile.domain.ResetUserDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    /** Operación destructiva en curso, conservada para poder cancelarla desde el diálogo. */
    private var operationJob: Job? = null

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
        _uiState.update { it.copy(selectedAction = action, error = null) }
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
                        error = null,
                    )
                }
            }
            SecurityActionType.DELETE_ACCOUNT -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = true,
                        reauthPassword = "",
                        isReauthPasswordEmpty = false,
                        error = null,
                    )
                }
            }
        }
    }

    /**
     * Cierra el diálogo de restablecimiento de datos del paso 2.
     *
     * Si la operación está en curso ([SecurityZoneUiState.isResettingData]) la cancela: el usuario
     * nunca queda atrapado esperando una confirmación del servidor que puede no llegar.
     */
    fun onDismissResetDialog() {
        cancelRunningOperation()
        _uiState.update {
            it.copy(showResetConfirmationDialog = false, isResettingData = false, error = null)
        }
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
                error = null,
            )
        }

        operationJob = viewModelScope.launch {
            when (val result = resetUserData()) {
                is KronoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isResettingData = false,
                            showResetConfirmationDialog = false,
                            isResetSuccessful = true,
                        )
                    }
                    _events.send(SecurityZoneEvent.DataResetSuccess)
                }
                is KronoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isResettingData = false,
                            error = result.throwable.toSecurityZoneError(),
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
     *
     * Si la operación está en curso ([SecurityZoneUiState.isDeletingAccount]) la cancela, por la
     * misma razón que [onDismissResetDialog].
     */
    fun onDismissDeleteDialog() {
        cancelRunningOperation()
        _uiState.update {
            it.copy(
                showDeleteAccountDialog = false,
                isDeletingAccount = false,
                reauthPassword = "",
                isReauthPasswordEmpty = false,
                error = null,
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
                isReauthPasswordEmpty = current.isReauthPasswordEmpty && password.isBlank(),
                error = null,
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
            _uiState.update { it.copy(isReauthPasswordEmpty = true) }
            return
        }

        _uiState.update {
            it.copy(
                isDeletingAccount = true,
                isReauthPasswordEmpty = false,
                error = null,
            )
        }

        operationJob = viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            error = result.throwable.toSecurityZoneError(),
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

    private fun cancelRunningOperation() {
        operationJob?.cancel()
        operationJob = null
    }

    private fun Throwable.toSecurityZoneError(): SecurityZoneError = when (this) {
        is InvalidPasswordException -> SecurityZoneError.WrongPassword
        is NetworkUnavailableException -> SecurityZoneError.NetworkUnavailable
        is RemoteTimeoutException -> SecurityZoneError.Timeout
        else -> SecurityZoneError.Unexpected(message)
    }
}
