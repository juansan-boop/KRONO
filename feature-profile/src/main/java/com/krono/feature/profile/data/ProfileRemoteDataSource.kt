package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs

/**
 * Contrato para la sincronización remota del perfil, preferencias y seguridad
 * con Cloud Firestore y Firebase Authentication (F-15, F-19, F-20, F-21).
 */
interface ProfileRemoteDataSource {

    /**
     * Sincroniza los datos del perfil (nombre, rol, avatar) en el documento `users/{userId}`
     * y actualiza el displayName en Firebase Authentication.
     */
    suspend fun saveUserProfile(
        userId: String,
        name: String,
        role: String,
        avatarUri: String?,
    ): KronoResult<Unit>

    /**
     * Sincroniza las preferencias de notificación en el documento `users/{userId}`.
     */
    suspend fun saveNotificationPrefs(
        userId: String,
        prefs: NotificationPrefs,
    ): KronoResult<Unit>

    /**
     * Sincroniza los interruptores de ajustes en el documento `users/{userId}`.
     */
    suspend fun saveSettingsToggles(
        userId: String,
        dynamicBreaks: Boolean,
        silenceNotifications: Boolean,
        priorityAlerts: Boolean,
    ): KronoResult<Unit>

    /**
     * Restablece los datos y configuración del documento `users/{userId}` a valores por defecto en Firestore.
     */
    suspend fun resetUserData(userId: String): KronoResult<Unit>

    /**
     * Reautentica al usuario con su contraseña actual, elimina su documento en Firestore
     * y da de baja la cuenta en Firebase Authentication (F-21 CA-2).
     */
    suspend fun deleteAccount(password: String): KronoResult<Unit>

    /**
     * Sincroniza bidireccionalmente los datos del documento remoto `users/{userId}` en Cloud Firestore
     * hacia el almacén local [localStore] (perfil, interruptores de ajustes y preferencias de notificación).
     */
    suspend fun syncFromRemote(
        userId: String,
        localStore: ProfileLocalStore,
    ): KronoResult<Unit>
}
