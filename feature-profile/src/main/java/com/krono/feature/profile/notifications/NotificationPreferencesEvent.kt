package com.krono.feature.profile.notifications

/**
 * Eventos de un solo uso emitidos por [NotificationPreferencesViewModel] hacia la UI.
 */
sealed interface NotificationPreferencesEvent {
    /** Las preferencias se guardaron exitosamente en el sistema (F-15 CA-2). */
    data object PreferencesSaved : NotificationPreferencesEvent

    /** Las preferencias se restablecieron a sus valores predeterminados (F-15 CA-2). */
    data object DefaultsRestored : NotificationPreferencesEvent

    /** Solicitud para invocar el launcher del permiso de notificaciones (F-15 CA-3). */
    data object RequestNotificationPermission : NotificationPreferencesEvent

    /** Navegación hacia atrás fuera de la pantalla de preferencias. */
    data object NavigateBack : NotificationPreferencesEvent
}
