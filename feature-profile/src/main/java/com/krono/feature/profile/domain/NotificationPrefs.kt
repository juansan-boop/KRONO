package com.krono.feature.profile.domain

/**
 * Modelo inmutable de preferencias de notificaciones y alarmas (F-15, Sección 6 de SPEC.md).
 *
 * @property deadlineReminderMinutes Minutos de anticipación para avisos de entrega (30 por defecto, CA-1).
 * @property deadlineRemindersEnabled Si las alertas de entrega se encuentran activas.
 * @property focusStartSound Si la alarma sonora al iniciar un bloque de concentración está activa.
 * @property soundTone Nombre o identificador del tono sonoro para el inicio de concentración.
 */
data class NotificationPrefs(
    val deadlineReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val deadlineRemindersEnabled: Boolean = true,
    val focusStartSound: Boolean = true,
    val soundTone: String = DEFAULT_SOUND_TONE,
) {
    companion object {
        /** Anticipación por defecto en minutos para avisos de entrega de tareas (CA-1, CA-2). */
        const val DEFAULT_REMINDER_MINUTES = 30

        /** Tono sonoro por defecto para el inicio de bloques de concentración. */
        const val DEFAULT_SOUND_TONE = "Chime"
    }
}
