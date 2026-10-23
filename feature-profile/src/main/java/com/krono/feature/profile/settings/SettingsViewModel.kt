package com.krono.feature.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.feature.profile.data.NoOpAuthRepository
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
 * @property localStore Almacén local persistente para reactividad y persistencia entre reinicios.
 * @property remoteDataSource Fuente remota para sincronizar configuraciones en Cloud Firestore (`users/{uid}`).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logout: LogoutUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val localStore: ProfileLocalStore,
    private val remoteDataSource: ProfileRemoteDataSource,
) : ViewModel() {

    constructor(
        logout: LogoutUseCase,
        observeSession: ObserveSessionUseCase,
        localStore: ProfileLocalStore,
    ) : this(
        logout = logout,
        observeSession = observeSession,
        localStore = localStore,
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    constructor(
        logout: LogoutUseCase,
        observeSession: ObserveSessionUseCase,
    ) : this(
        logout = logout,
        observeSession = observeSession,
        localStore = ProfileLocalStore(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    constructor(
        logout: LogoutUseCase,
        localStore: ProfileLocalStore,
    ) : this(
        logout = logout,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        localStore = localStore,
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    constructor(logout: LogoutUseCase) : this(
        logout = logout,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        localStore = ProfileLocalStore(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            userName = localStore.userName.value,
            userRole = localStore.userRole.value,
            userEmail = localStore.userEmail.value,
            avatarUri = localStore.avatarUri.value,
            userInitials = calculateInitials(localStore.userName.value),
            dynamicBreaksEnabled = localStore.dynamicBreaksEnabled.value,
            silenceInternalNotificationsEnabled = localStore.silenceInternalNotificationsEnabled.value,
            priorityAlertsEnabled = localStore.priorityAlertsEnabled.value,
        ),
    )

    /** Flujo de estado visual observable para la UI. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)

    /** Canal de eventos de un solo uso para navegación desacoplada. */
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { user ->
                if (user != null) {
                    localStore.bindUser(user.id, user.email)
                    remoteDataSource.syncFromRemote(user.id, localStore)
                }
            }
        }

        viewModelScope.launch {
            combine(
                localStore.userName,
                localStore.userRole,
                localStore.userEmail,
                localStore.avatarUri,
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
                localStore.dynamicBreaksEnabled,
                localStore.silenceInternalNotificationsEnabled,
                localStore.priorityAlertsEnabled,
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
        localStore.setDynamicBreaksEnabled(enabled)
        _uiState.update { it.copy(dynamicBreaksEnabled = enabled) }
        syncSettingsToggles()
    }

    /**
     * Alterna el silenciado de notificaciones internas durante sesiones de concentración.
     *
     * @param enabled Nuevo valor del interruptor.
     */
    fun onSilenceNotificationsToggle(enabled: Boolean) {
        localStore.setSilenceInternalNotificationsEnabled(enabled)
        _uiState.update { it.copy(silenceInternalNotificationsEnabled = enabled) }
        syncSettingsToggles()
    }

    /**
     * Alterna la activación de alertas prioritarias P1 antes de un deadline crítico.
     *
     * @param enabled Nuevo valor del interruptor.
     */
    fun onPriorityAlertsToggle(enabled: Boolean) {
        localStore.setPriorityAlertsEnabled(enabled)
        _uiState.update { it.copy(priorityAlertsEnabled = enabled) }
        syncSettingsToggles()
    }

    private fun syncSettingsToggles() {
        viewModelScope.launch {
            remoteDataSource.saveSettingsToggles(
                userId = localStore.activeUserId,
                dynamicBreaks = localStore.dynamicBreaksEnabled.value,
                silenceNotifications = localStore.silenceInternalNotificationsEnabled.value,
                priorityAlerts = localStore.priorityAlertsEnabled.value,
            )
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
                logoutErrorMessage = null,
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
                logoutErrorMessage = null,
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
                logoutErrorMessage = null,
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
                    val fallbackMessage = "No fue posible cerrar la sesión. Intenta de nuevo."
                    val errorMessage = when (val throwable = result.throwable) {
                        is AuthError.NoConnection -> "No hay conexión con el servidor."
                        is AuthError.TooManyRequests -> "Demasiados intentos. Espera unos momentos."
                        else -> throwable.message?.takeIf { it.isNotBlank() } ?: fallbackMessage
                    }
                    _uiState.update {
                        it.copy(
                            isLoggingOut = false,
                            logoutErrorMessage = errorMessage,
                        )
                    }
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    private fun calculateInitials(name: String): String =
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "K" }

    private data class ProfileIdentity(
        val name: String,
        val role: String,
        val email: String,
        val avatar: String?,
    )
}
