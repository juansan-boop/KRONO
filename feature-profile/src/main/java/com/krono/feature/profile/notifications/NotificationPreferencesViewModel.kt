package com.krono.feature.profile.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.domain.NotificationPrefs
import com.krono.feature.profile.domain.ResetNotificationPrefsUseCase
import com.krono.feature.profile.domain.SaveNotificationPrefsUseCase
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
 * ViewModel que gobierna la pantalla de Notificaciones y Alarmas (F-15 CA-1, CA-2, CA-3).
 *
 * Administra el estado inmutable [NotificationPreferencesUiState], el tiempo de anticipación de entrega
 * (30 minutos por defecto), el interruptor de avisos de entrega, el sonido de inicio de concentración,
 * el estado del permiso de notificaciones del sistema, el bloqueo de doble clic concurrente durante
 * el guardado y la restauración integral de valores predeterminados.
 *
 * Regla dura de arquitectura de KRONO: NO importa `androidx.compose.*`.
 *
 * @property saveNotificationPrefs Caso de uso para validar y guardar las preferencias.
 * @property resetNotificationPrefs Caso de uso para restablecer las preferencias a valores por defecto.
 * @property localStore Almacén local persistente para cargar las preferencias guardadas.
 */
@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val saveNotificationPrefs: SaveNotificationPrefsUseCase,
    private val resetNotificationPrefs: ResetNotificationPrefsUseCase,
    private val localStore: ProfileLocalStore,
) : ViewModel() {

    constructor(
        saveNotificationPrefs: SaveNotificationPrefsUseCase,
        resetNotificationPrefs: ResetNotificationPrefsUseCase,
    ) : this(saveNotificationPrefs, resetNotificationPrefs, ProfileLocalStore())

    private val _uiState = MutableStateFlow(
        NotificationPreferencesUiState(
            deadlineReminderMinutes = localStore.deadlineReminderMinutes.value,
            deadlineRemindersEnabled = localStore.deadlineRemindersEnabled.value,
            focusStartSound = localStore.focusStartSound.value,
            soundTone = localStore.soundTone.value,
        ),
    )


    /** Flujo de estado visual inmutable observable por la pantalla. */
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificationPreferencesEvent>(Channel.BUFFERED)

    /** Canal de eventos de un solo uso para acciones desacopladas de navegación y permisos. */
    val events: Flow<NotificationPreferencesEvent> = _events.receiveAsFlow()

    /**
     * Actualiza los minutos de anticipación para avisos de entrega de tareas (F-15 CA-1).
     *
     * @param minutes Cantidad de minutos configurados (por defecto 30).
     */
    fun onDeadlineReminderMinutesChange(minutes: Int) {
        _uiState.update { current ->
            current.copy(
                deadlineReminderMinutes = minutes,
                saveError = null,
            )
        }
    }

    /**
     * Alterna la activación general de los avisos de entrega de tareas.
     *
     * @param enabled `true` para activar avisos, `false` para silenciar.
     */
    fun onDeadlineRemindersEnabledChange(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                deadlineRemindersEnabled = enabled,
                saveError = null,
            )
        }
    }

    /**
     * Alterna la reproducción de alarma sonora al iniciar bloques de concentración.
     *
     * @param enabled `true` para reproducir sonido discreto, `false` para modo silencioso.
     */
    fun onFocusStartSoundChange(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                focusStartSound = enabled,
                saveError = null,
            )
        }
    }

    /**
     * Actualiza el tono sonoro configurado para las alertas de concentración.
     *
     * @param tone Nombre o identificador del tono.
     */
    fun onSoundToneChange(tone: String) {
        _uiState.update { current ->
            current.copy(
                soundTone = tone,
                saveError = null,
            )
        }
    }

    /**
     * Procesa el resultado de la solicitud del permiso de notificaciones (F-15 CA-3).
     *
     * Si el usuario deniega el permiso (`isGranted == false`), el estado de UI se actualiza
     * adecuadamente sin lanzar excepciones ni bloquear la funcionalidad ni la navegación de la pantalla.
     *
     * @param isGranted `true` si el usuario otorgó el permiso, `false` en caso contrario.
     */
    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { current ->
            current.copy(
                isPermissionGranted = isGranted,
                saveError = null,
            )
        }
    }

    /**
     * Solicita invocar el diálogo del sistema operativo para requerir el permiso POST_NOTIFICATIONS.
     */
    fun onRequestPermissionClick() {
        viewModelScope.launch {
            _events.send(NotificationPreferencesEvent.RequestNotificationPermission)
        }
    }

    /**
     * Emite el evento para regresar a la pantalla anterior (Ajustes).
     */
    fun onBackClick() {
        viewModelScope.launch {
            _events.send(NotificationPreferencesEvent.NavigateBack)
        }
    }

    /**
     * Restablece las preferencias a los valores por defecto del sistema (F-15 CA-2).
     *
     * Regresa `deadlineReminderMinutes` a 30 y restaura los valores base de avisos y sonido.
     */
    fun onResetToDefaultsClick() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            when (val result = resetNotificationPrefs()) {
                is KronoResult.Success -> {
                    val defaults = result.data
                    _uiState.update { current ->
                        current.copy(
                            deadlineReminderMinutes = defaults.deadlineReminderMinutes,
                            deadlineRemindersEnabled = defaults.deadlineRemindersEnabled,
                            focusStartSound = defaults.focusStartSound,
                            soundTone = defaults.soundTone,
                            saveError = null,
                        )
                    }
                    _events.send(NotificationPreferencesEvent.DefaultsRestored)
                }
                is KronoResult.Error -> {
                    _uiState.update { it.copy(saveError = result.throwable.toProfileSaveError(OPERATION_RESET_PREFS)) }
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Guarda las preferencias de notificación en el sistema (F-15 CA-2).
     *
     * Previene envíos concurrentes si [NotificationPreferencesUiState.isSaving] ya está activo.
     */
    fun onSaveClick() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val prefs = NotificationPrefs(
                deadlineReminderMinutes = currentState.deadlineReminderMinutes,
                deadlineRemindersEnabled = currentState.deadlineRemindersEnabled,
                focusStartSound = currentState.focusStartSound,
                soundTone = currentState.soundTone,
            )

            when (val result = saveNotificationPrefs(prefs)) {
                is KronoResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                    _events.send(NotificationPreferencesEvent.PreferencesSaved)
                }
                is KronoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSuccess = false,
                            saveError = result.throwable.toProfileSaveError(OPERATION_SAVE_PREFS),
                        )
                    }
                }
                KronoResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val OPERATION_SAVE_PREFS = "guardar preferencias de notificación"
        const val OPERATION_RESET_PREFS = "restablecer preferencias de notificación"
    }
}
