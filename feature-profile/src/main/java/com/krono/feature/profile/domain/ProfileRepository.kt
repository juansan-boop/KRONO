package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio unificado para la gestión y sincronización del perfil de usuario,
 * preferencias de notificación y configuraciones de seguridad (F-15, F-19, F-20, F-21).
 */
interface ProfileRepository {

    /** Nombre visible del usuario activo. */
    val userName: StateFlow<String>

    /** Rol o cargo profesional/académico del usuario. */
    val userRole: StateFlow<String>

    /** Correo electrónico autenticado del usuario. */
    val userEmail: StateFlow<String>

    /** URI o ruta local del avatar seleccionado. */
    val avatarUri: StateFlow<String?>

    /** Interruptor de pausas dinámicas según ritmo circadiano. */
    val dynamicBreaksEnabled: StateFlow<Boolean>

    /** Interruptor de silenciar notificaciones internas durante concentración. */
    val silenceInternalNotificationsEnabled: StateFlow<Boolean>

    /** Interruptor de alertas prioritarias P1. */
    val priorityAlertsEnabled: StateFlow<Boolean>

    /** Minutos de anticipación de recordatorios de vencimiento. */
    val deadlineReminderMinutes: StateFlow<Int>

    /** Interruptor general de recordatorios de vencimiento. */
    val deadlineRemindersEnabled: StateFlow<Boolean>

    /** Interruptor de sonido de inicio de concentración. */
    val focusStartSound: StateFlow<Boolean>

    /** Tono de notificación configurado. */
    val soundTone: StateFlow<String>

    /** Estado explícito de la última sincronización con Cloud Firestore. */
    val syncState: StateFlow<SyncState>

    /** Identificador único del usuario activo en la sesión. */
    val activeUserId: String

    /** Comprueba si el dispositivo cuenta con conectividad a internet activa. */
    fun isOnline(): Boolean

    /** Vincula la sesión con el usuario actual y carga sus preferencias locales. */
    fun bindUser(userId: String, email: String)

    /** Sincroniza los datos remotos de Cloud Firestore hacia el almacenamiento local. */
    suspend fun syncFromRemote(userId: String): KronoResult<Unit>

    /** Actualiza los datos del perfil (nombre, rol, avatar) tanto local como remotamente. */
    suspend fun updateProfile(name: String, role: String, avatarUri: String? = null): KronoResult<Unit>

    /** Persiste la URI del avatar en el almacenamiento local. */
    fun saveAvatarUri(uri: String?)

    /** Guarda el interruptor de descansos dinámicos en el almacén local. */
    fun setDynamicBreaksEnabled(enabled: Boolean)

    /** Guarda el interruptor de silenciado de notificaciones internas en el almacén local. */
    fun setSilenceInternalNotificationsEnabled(enabled: Boolean)

    /** Guarda el interruptor de alertas prioritarias en el almacén local. */
    fun setPriorityAlertsEnabled(enabled: Boolean)

    /** Sincroniza los interruptores de ajustes hacia Cloud Firestore. */
    suspend fun syncSettingsToggles(): KronoResult<Unit>

    /** Guarda y sincroniza las preferencias de notificación provistas. */
    suspend fun saveNotificationPrefs(prefs: NotificationPrefs): KronoResult<Unit>

    /** Restablece y sincroniza las preferencias de notificación a sus valores de fábrica. */
    suspend fun resetNotificationPrefs(): KronoResult<NotificationPrefs>

    /** Obtiene una copia inmutable de las preferencias de notificación actuales en memoria. */
    fun getNotificationPrefs(): NotificationPrefs

    /** Reintenta sincronizar los cambios locales pendientes o rechazados hacia Cloud Firestore. */
    suspend fun retrySync(): KronoResult<Unit>

    /** Restablece datos y configuración del usuario conservando la cuenta activa (F-21 CA-1). */
    suspend fun resetUserData(): KronoResult<Unit>

    /** Reautentica al usuario con su contraseña actual antes de operaciones destructivas (F-21 CA-2). */
    suspend fun reauthenticate(password: String): KronoResult<Unit>

    /** Elimina definitivamente los datos y la cuenta de usuario en Firebase (F-21 CA-2). */
    suspend fun deleteAccount(password: String): KronoResult<Unit>

    /** Restablece la caché local a los valores por defecto del sistema. */
    fun resetToDefaults()
}
