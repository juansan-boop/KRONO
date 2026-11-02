package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs

/**
 * Contrato para la sincronización remota del perfil, preferencias y seguridad
 * con Cloud Firestore y Firebase Authentication (F-15, F-19, F-20, F-21).
 *
 * Las tres operaciones de guardado ([saveUserProfile], [saveNotificationPrefs], [saveSettingsToggles])
 * esperan la confirmación del servidor solo durante un tiempo acotado: Firestore ya aplicó la
 * escritura en su caché local y la tiene encolada, así que si el servidor no responde a tiempo (sin
 * conexión) devuelven [KronoResult.Success]. Los errores reales (permiso denegado, datos inválidos)
 * llegan como [KronoResult.Error].
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
     * Restablece los datos del usuario (F-21 CA-1): borra sus tareas
     * (`users/{userId}/tasks`) y el cronómetro activo (`users/{userId}/timer/active`) y deja el
     * documento `users/{userId}` con sus valores por defecto.
     *
     * El cronómetro y el perfil se escriben juntos en el último lote atómico. Con más tareas de las
     * que caben en un lote, un fallo en un lote previo deja tareas parcialmente borradas con el perfil
     * y el cronómetro intactos; reintentar converge. Si un lote no se confirma dentro del tiempo
     * límite devuelve [com.krono.feature.profile.domain.RemoteTimeoutException].
     */
    suspend fun resetUserData(userId: String): KronoResult<Unit>

    /**
     * Reautentica al usuario con su contraseña actual (F-21 CA-2).
     *
     * Debe ejecutarse y resolverse con éxito ANTES de cualquier borrado destructivo: si falla
     * (contraseña incorrecta o sesión no reciente), no se debe eliminar ningún dato.
     */
    suspend fun reauthenticate(password: String): KronoResult<Unit>

    /**
     * Elimina los datos del usuario (tareas, `timer/active` y el documento `users/{userId}`, estos
     * dos últimos en el lote final atómico) y, solo si todo se confirmó, da de baja la cuenta en
     * Firebase Authentication (F-21 CA-2). Requiere una reautenticación previa con [reauthenticate].
     *
     * No borra `timeSessions` ni otras subcolecciones (sin borrado en cascada).
     */
    suspend fun deleteAccount(userId: String): KronoResult<Unit>

    /**
     * Sincroniza bidireccionalmente los datos del documento remoto `users/{userId}` en Cloud Firestore
     * hacia el almacén local [localStore] (perfil, interruptores de ajustes y preferencias de notificación).
     */
    suspend fun syncFromRemote(
        userId: String,
        localStore: ProfileLocalStore,
    ): KronoResult<Unit>
}
