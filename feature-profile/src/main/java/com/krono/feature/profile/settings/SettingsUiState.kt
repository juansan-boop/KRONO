package com.krono.feature.profile.settings

/**
 * Estado inmutable de la pantalla de Ajustes y Perfil (F-19, F-20, F-25 CA-4).
 *
 * Centraliza la información del perfil del usuario, los interruptores del hub
 * de ajustes, la configuración de sesiones y el control del diálogo de confirmación
 * de cierre de sesión.
 *
 * @property userName Nombre completo del usuario para la tarjeta de identidad.
 * @property userEmail Correo electrónico asociado a la cuenta activa.
 * @property userRole Rol académico y laboral (ej. "Estudiante y trabajador").
 * @property avatarUri URI local de la foto de perfil o `null` si se usan iniciales.
 * @property isSyncActive Indica si la sincronización en la nube con KRONO AI está activa.
 * @property syncStatusText Texto descriptivo del estado de sincronización (ej. "100% al día").
 * @property userInitials Iniciales generadas para el avatar en caso de ausencia de foto.
 * @property dynamicBreaksEnabled Sugerencia dinámica de descansos según ritmo circadiano.
 * @property silenceInternalNotificationsEnabled Silenciar notificaciones durante sesiones de concentración.
 * @property activePresetsCount Cantidad de plantillas de concentración configuradas.
 * @property priorityAlertsEnabled Alertas prioritarias P1 antes de deadlines críticos.
 * @property currentThemeName Nombre del tema visual activo (ej. "Cyber Dark").
 * @property appVersionText Identificador de build y versión del sistema.
 * @property showLogoutConfirmationDialog Controla la visibilidad del diálogo oficial de cierre de sesión.
 * @property isLoggingOut Indica si la operación de cierre de sesión está en proceso.
 * @property logoutErrorMessage Mensaje de error si la invocación de cierre de sesión falla.
 */
data class SettingsUiState(
    val userName: String = "Usuario",
    val userEmail: String = "",
    val userRole: String = "Estudiante y trabajador",
    val avatarUri: String? = null,
    val isSyncActive: Boolean = true,
    val syncStatusText: String = "100% al día",
    val userInitials: String = "U",
    val dynamicBreaksEnabled: Boolean = true,
    val silenceInternalNotificationsEnabled: Boolean = true,
    val activePresetsCount: Int = 3,
    val priorityAlertsEnabled: Boolean = true,
    val currentThemeName: String = "Cyber Dark",
    val appVersionText: String = "KRONO Cyber Productivity // Build 2.4.12",
    val showLogoutConfirmationDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutErrorMessage: String? = null,
)
