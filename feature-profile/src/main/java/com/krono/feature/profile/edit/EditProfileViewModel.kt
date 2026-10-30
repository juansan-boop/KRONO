package com.krono.feature.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.feature.profile.data.NoOpAuthRepository
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.domain.UpdateProfileUseCase
import com.krono.feature.profile.ui.toProfileSaveError
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
 * ViewModel que gobierna la pantalla de Edición de Perfil (F-19 CA-2).
 *
 * Administra el estado inmutable [EditProfileUiState], valida los campos requeridos
 * antes de invocar la persistencia, previene el doble envío mientras la operación
 * está activa ([EditProfileUiState.isSaving]) y emite eventos de un solo uso [EditProfileEvent].
 *
 * Cumple estrictamente la regla de arquitectura de KRONO: no importa `androidx.compose.*`.
 *
 * @property updateProfile Caso de uso para validar y persistir los cambios de perfil.
 * @property observeSession Caso de uso para vincular la cuenta activa de Firebase Authentication.
 * @property localStore Almacén local persistente para cargar los datos de perfil actuales.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val updateProfile: UpdateProfileUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val localStore: ProfileLocalStore,
) : ViewModel() {

    constructor(
        updateProfile: UpdateProfileUseCase,
        observeSession: ObserveSessionUseCase,
    ) : this(
        updateProfile = updateProfile,
        observeSession = observeSession,
        localStore = ProfileLocalStore(),
    )

    constructor(
        updateProfile: UpdateProfileUseCase,
        localStore: ProfileLocalStore,
    ) : this(
        updateProfile = updateProfile,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        localStore = localStore,
    )

    constructor(updateProfile: UpdateProfileUseCase) : this(
        updateProfile = updateProfile,
        observeSession = ObserveSessionUseCase(NoOpAuthRepository()),
        localStore = ProfileLocalStore(),
    )

    private val _uiState = MutableStateFlow(
        EditProfileUiState(
            name = localStore.userName.value,
            role = localStore.userRole.value,
            email = localStore.userEmail.value,
            avatarUri = localStore.avatarUri.value,
        ),
    )

    /** Flujo de estado visual inmutable observable por la UI. */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)

    /** Canal de eventos de un solo uso para navegación desacoplada. */
    val events: Flow<EditProfileEvent> = _events.receiveAsFlow()

    private var isInitializedForUser: String? = null

    init {
        viewModelScope.launch {
            observeSession().collect { user ->
                if (user != null) {
                    localStore.bindUser(user.id, user.email)
                    if (isInitializedForUser != user.id) {
                        isInitializedForUser = user.id
                        _uiState.update { current ->
                            current.copy(
                                email = user.email,
                                name = if (current.name.isBlank() || current.name == ProfileLocalStore.DEFAULT_NAME) {
                                    localStore.userName.value
                                } else {
                                    current.name
                                },
                                role = if (current.role.isBlank()) localStore.userRole.value else current.role,
                                avatarUri = current.avatarUri ?: localStore.avatarUri.value,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Actualiza el valor del nombre y limpia los errores activos si el nuevo valor no está en blanco.
     *
     * @param newName Nuevo texto del nombre completo ingresado.
     */
    fun onNameChange(newName: String) {
        _uiState.update { current ->
            current.copy(
                name = newName,
                isNameMissing = current.isNameMissing && newName.isBlank(),
                saveError = null,
            )
        }
    }

    /**
     * Actualiza el rol académico y profesional seleccionado.
     *
     * @param newRole Nuevo rol seleccionado de la lista disponible.
     */
    fun onRoleChange(newRole: String) {
        _uiState.update { current ->
            current.copy(
                role = newRole,
                saveError = null,
            )
        }
    }

    /**
     * Actualiza la foto de perfil seleccionada desde la galería local.
     *
     * @param uri Cadena que representa el URI de la imagen seleccionada.
     */
    fun onAvatarSelected(uri: String) {
        _uiState.update { current ->
            current.copy(
                avatarUri = uri,
                saveError = null,
            )
        }
    }

    /**
     * Cancela la edición y emite el evento para regresar a la pantalla anterior sin guardar.
     */
    fun onCancel() {
        viewModelScope.launch {
            _events.send(EditProfileEvent.NavigateBack)
        }
    }

    /**
     * Valida los campos obligatorios y persiste los cambios del perfil del usuario (F-19 CA-2).
     */
    fun onSave() {
        val currentState = _uiState.value

        // Protección contra pulsaciones repetidas o concurrentes
        if (currentState.isSaving) return

        val trimmedName = currentState.name.trim()
        val trimmedRole = currentState.role.trim()

        // Validación de campo obligatorio
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(isNameMissing = true) }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                name = trimmedName,
                role = trimmedRole,
                isNameMissing = false,
                saveError = null,
            )
        }

        viewModelScope.launch {
            when (val result = updateProfile(trimmedName, trimmedRole, currentState.avatarUri)) {
                is KronoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSuccess = true,
                        )
                    }
                    _events.send(EditProfileEvent.ProfileSaved)
                }
                is KronoResult.Error -> {
                    val error = result.throwable.toProfileSaveError(OPERATION_SAVE_PROFILE)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = error,
                        )
                    }
                }
                is KronoResult.Loading -> {
                    _uiState.update { it.copy(isSaving = true) }
                }
            }
        }
    }

    private companion object {
        const val OPERATION_SAVE_PROFILE = "guardar perfil"
    }
}
