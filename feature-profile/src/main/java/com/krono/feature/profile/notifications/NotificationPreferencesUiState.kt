package com.krono.feature.profile.notifications

import com.krono.feature.profile.domain.NotificationPrefs
import com.krono.feature.profile.ui.ProfileSaveError

/**
 * Estado inmutable de la pantalla de Preferencias de Notificaciones y Alarmas (F-15 CA-1, CA-2, CA-3).
 *
 * @property deadlineReminderMinutes Minutos de anticipación configurados para avisos de entrega (30 por defecto, CA-1).
 * @property deadlineRemindersEnabled Interruptor para activar o silenciar avisos de entrega.
 * @property focusStartSound Interruptor para reproducir alarma sonora al iniciar bloques de concentración.
 * @property soundTone Nombre o identificador del tono sonoro actual.
 * @property isPermissionGranted Estado del permiso del sistema POST_NOTIFICATIONS (CA-3).
 * @property isSaving Indicador de operación de guardado en curso (bloquea envíos concurrentes).
 * @property isSuccess Indicador de operación completada con éxito.
 * @property saveError Causa tipada del fallo al guardar o restablecer (la UI la traduce a copy), o `null` si no hay.
 */
data class NotificationPreferencesUiState(
    val deadlineReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val deadlineRemindersEnabled: Boolean = true,
    val focusStartSound: Boolean = true,
    val soundTone: String = NotificationPrefs.DEFAULT_SOUND_TONE,
    val isPermissionGranted: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val saveError: ProfileSaveError? = null,
) {
    companion object {
        /** Anticipación por defecto en minutos (F-15 CA-1, CA-2). */
        const val DEFAULT_REMINDER_MINUTES = NotificationPrefs.DEFAULT_REMINDER_MINUTES

        /** Opciones de anticipación configurables en minutos. */
        val AVAILABLE_REMINDER_MINUTES = listOf(10, 15, 30, 60)
    }
}
