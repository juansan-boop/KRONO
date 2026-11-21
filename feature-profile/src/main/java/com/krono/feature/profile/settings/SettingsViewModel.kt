package com.krono.feature.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.feature.profile.data.NoOpAuthRepository
import com.krono.feature.profile.data.ProfileRepositoryImpl
import com.krono.feature.profile.domain.ProfileRepository
import com.krono.feature.profile.ui.ProfileLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Eventos de navegación y acciones de un solo uso emitidos desde el Hub de Ajustes.
 */
sealed interface SettingsEvent {
    /** El cierre de sesión se completó con éxito; la app debe navegar a la pantalla de autenticación. */
    data object LoggedOut : SettingsEvent
}

/**
 * ViewModel encargado del Hub de Ajustes y Perfil (F-19, F-20, F-25 CA-4).
 *
 * Administra el estado visual inmutable [SettingsUiState], la interacción con los
 * interruptores de configuración y el flujo de cierre de sesión oficial mediante [LogoutUseCase].
 *
 * Cumple estrictamente la regla de arquitectura de KRONO: no importa `androidx.compose.*`.
 *
 * @property logout Caso de uso oficial de dominio para cerrar la sesión activa.
 * @property observeSession Caso de uso para observar la identidad del usuario autenticado en Firebase.
 * @property profileRepository Repositorio unificado para gestión del perfil, preferencias y sincronización remota.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logout: LogoutUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    constructor(
        logout: LogoutUseCase,
        observeSession: ObserveSessionUseCase,
    ) : this(
        logout = logout,
        observeSession = observeSession,
        profileRepository = ProfileRepositoryImpl(),
    )

    constructor(
        logout: LogoutUseCase,
        profileRepository: ProfileRepository,
    ) : this(
        logout = logout,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        profileRepository = profileRepository,
    )

    constructor(logout: LogoutUseCase) : this(
        logout = logout,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        profileRepository = ProfileRepositoryImpl(),
    )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            userName = profileRepository.userName.value,
            userRole = profileRepository.userRole.value,
            userEmail = profileRepository.userEmail.value,
            avatarUri = profileRepository.avatarUri.value,
            userInitials = calculateInitials(profileRepository.userName.value),
            dynamicBreaksEnabled = profileRepository.dynamicBreaksEnabled.value,
            silenceInternalNotificationsEnabled = profileRepository.silenceInternalNotificationsEnabled.value,
            priorityAlertsEnabled = profileRepository.priorityAlertsEnabled.value,
            syncState = profileRepository.syncState.value,
        ),
    )

    /** Flujo de estado visual observable para la UI. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)

    /** Canal de eventos de un solo uso para navegación desacoplada. */
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            profileRepository.syncState.collect { syncState ->
                _uiState.update { it.copy(syncState = syncState) }
            }
        }

        viewModelScope.launch {
            observeSession().collect { user ->
                if (user != null) {
                    profileRepository.bindUser(user.id, user.email)
                    profileRepository.syncFromRemote(user.id)
                }
            }
        }

        viewModelScope.launch {
            combine(
                profileRepository.userName,
                profileRepository.userRole,
                profileRepository.userEmail,
                profileRepository.avatarUri,
            ) { name, role, email, avatar ->
                ProfileIdentity(name, role, email, avatar)
            }.collect { identity ->
                _uiState.update { current ->
                    current.copy(
                        userName = identity.name,
                        userRole = identity.role,
                        userEmail = identity.email,
                        avatarUri = identity.avatar,
                        userInitials = calculateInitials(identity.name),
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                profileRepository.dynamicBreaksEnabled,
                profileRepository.silenceInternalNotificationsEnabled,
                profileRepository.priorityAlertsEnabled,
            ) { dynamicBreaks, silenceNotifs, priorityAlerts ->
                Triple(dynamicBreaks, silenceNotifs, priorityAlerts)
            }.collect { (dynamicBreaks, silenceNotifs, priorityAlerts) ->
                _uiState.update { current ->
                    current.copy(
                        dynamicBreaksEnabled = dynamicBreaks,
                        silenceInternalNotificationsEnabled = silenceNotifs,
                        priorityAlertsEnabled = priorityAlerts,
                    )
                }
            }
        }
    }

    /**
     * Alterna la preferencia de pausas dinámicas según ritmo circadiano.
     *
     * @param enabled Nuevo valor del interruptor.
     */
    fun onDynamicBreaksToggle(enabled: Boolean) {
        profileRepository.setDynamicBreaksEnabled(enabled)
        _uiState.update { it.copy(dynamicBreaksEnabled = enabled) }
        syncSettingsToggles()
    }

    /**
     * Alterna el silenciado de notificaciones internas durante sesiones de concentración.
     *
     * @param enabled Nuevo valor del interruptor.
     */
    fun onSilenceNotificationsToggle(enabled: Boolean) {
        profileRepository.setSilenceInternalNotificationsEnabled(enabled)
        _uiState.update { it.copy(silenceInternalNotificationsEnabled = enabled) }
        syncSettingsToggles()
    }

    /**
     * Alterna la activación de alertas prioritarias P1 antes de un deadline crítico.
     *
     * @param enabled Nuevo valor del interruptor.
     */
    fun onPriorityAlertsToggle(enabled: Boolean) {
        profileRepository.setPriorityAlertsEnabled(enabled)
        _uiState.update { it.copy(priorityAlertsEnabled = enabled) }
        syncSettingsToggles()
    }

    private fun syncSettingsToggles() {
        viewModelScope.launch {
            profileRepository.syncSettingsToggles()
        }
    }

    /**
     * Reintenta sincronizar con Cloud Firestore los cambios locales pendientes o rechazados.
     */
    fun retrySync() {
        viewModelScope.launch {
            profileRepository.retrySync()
        }
    }

    /**
     * Abre el diálogo oficial de confirmación de cierre de sesión (F-25 CA-4).
     * Limpia cualquier mensaje de error previo.
     */
    fun onRequestLogout() {
        _uiState.update {
            it.copy(
                showLogoutConfirmationDialog = true,
                logoutError = null,
            )
        }
    }

    /**
     * Descarta el diálogo de cierre de sesión ("Permanecer en KRONO").
     * Si la operación ya está en progreso, la acción es ignorada para proteger la integridad.
     */
    fun onDismissLogoutDialog() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update {
            it.copy(
                showLogoutConfirmationDialog = false,
                logoutError = null,
            )
        }
    }

    /**
     * Confirma y ejecuta el cierre de sesión mediante [LogoutUseCase].
     *
     * - Bloquea interacciones concurrentes activando [SettingsUiState.isLoggingOut].
     * - Al completar con éxito, oculta el diálogo y emite [SettingsEvent.LoggedOut].
     * - Ante una falla, conserva el diálogo abierto, expone el mensaje de error y desbloquea los botones.
     */
    fun onConfirmLogout() {
        val current = _uiState.value
        if (current.isLoggingOut) return

        _uiState.update {
            it.copy(
                isLoggingOut = true,
                logoutError = null,
            )
        }

        viewModelScope.launch {
            when (val result = logout()) {
                is KronoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoggingOut = false,
                            showLogoutConfirmationDialog = false,
                        )
                    }
                    _events.send(SettingsEvent.LoggedOut)
                }
                is KronoResult.Error -> {
                    val error = result.throwable.toLogoutError()
                    _uiState.update {
                        it.copy(
                            isLoggingOut = false,
                            logoutError = error,
                        )
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    private fun Throwable.toLogoutError(): LogoutError = when (this) {
        is AuthError.NoConnection -> LogoutError.NO_CONNECTION
        is AuthError.TooManyRequests -> LogoutError.TOO_MANY_REQUESTS
        else -> {
            ProfileLog.failure(OPERATION_LOGOUT, this)
            LogoutError.UNEXPECTED
        }
    }

    private fun calculateInitials(name: String): String =
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "K" }

    private companion object {
        const val OPERATION_LOGOUT = "cerrar sesión"
    }

    private data class ProfileIdentity(
        val name: String,
        val role: String,
        val email: String,
        val avatar: String?,
    )
}
