package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso desacoplado dentro de `:feature-profile` para validar y persistir
 * las preferencias de notificaciones y alarmas (F-15 CA-2).
 *
 * Cumple con el aislamiento de arquitectura (D-8) y maneja errores retornando [KronoResult],
 * sincronizando cambios tanto en el almacén local como en Cloud Firestore (`users/{uid}`).
 */
open class SaveNotificationPrefsUseCase @Inject constructor(
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
     * Valida y guarda las preferencias de notificación provistas.
     *
     * @param prefs Objeto [NotificationPrefs] con las configuraciones a persistir.
     * @return [KronoResult.Success] si se guardaron correctamente, o [KronoResult.Error]
     * con [IllegalArgumentException] si la configuración no cumple con las restricciones mínimas.
     */
    open suspend operator fun invoke(prefs: NotificationPrefs): KronoResult<Unit> {
        if (prefs.deadlineReminderMinutes <= 0) {
            return KronoResult.Error(IllegalArgumentException("Los minutos de anticipación deben ser mayores a 0"))
        }
        localStore.saveNotificationPrefs(prefs)
        try {
            remoteDataSource.saveNotificationPrefs(
                userId = localStore.activeUserId,
                prefs = prefs,
            )
        } catch (_: Throwable) {
            // Sincronización remota tolerante a fallos
        }
        return KronoResult.Success(Unit)
    }
}

