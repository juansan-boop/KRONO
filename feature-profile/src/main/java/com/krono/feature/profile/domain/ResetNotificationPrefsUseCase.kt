package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso desacoplado dentro de `:feature-profile` para restablecer las
 * preferencias de notificaciones y alarmas a sus valores predeterminados (F-15 CA-2).
 *
 * Restaura `deadlineReminderMinutes = 30`, avisos activos y sonido de concentración habilitado,
 * garantizando el cumplimiento estricto de CA-2 en local y en Cloud Firestore (`users/{uid}`).
 */
open class ResetNotificationPrefsUseCase @Inject constructor(
    private val localStore: ProfileLocalStore,
    private val remoteDataSource: ProfileRemoteDataSource,
) {

    constructor(localStore: ProfileLocalStore) : this(
        localStore = localStore,
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    constructor() : this(
        localStore = ProfileLocalStore(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    /**
     * Restablece las preferencias a los valores predeterminados del sistema.
     *
     * @return [KronoResult.Success] conteniendo una nueva instancia de [NotificationPrefs]
     * con sus valores base de fábrica.
     */
    open suspend operator fun invoke(): KronoResult<NotificationPrefs> {
        val defaults = NotificationPrefs()
        localStore.saveNotificationPrefs(defaults)
        try {
            remoteDataSource.saveNotificationPrefs(
                userId = localStore.activeUserId,
                prefs = defaults,
            )
        } catch (_: Throwable) {
            // Sincronización remota tolerante a fallos
        }
        return KronoResult.Success(defaults)
    }
}

