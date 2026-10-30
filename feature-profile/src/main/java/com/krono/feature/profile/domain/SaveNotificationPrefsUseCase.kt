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
     * con [InvalidReminderMinutesException] si los minutos de anticipación no son mayores que cero.
     */
    open suspend operator fun invoke(prefs: NotificationPrefs): KronoResult<Unit> {
        if (prefs.deadlineReminderMinutes <= 0) {
            return KronoResult.Error(InvalidReminderMinutesException())
        }
        localStore.saveNotificationPrefs(prefs)
        syncRemoteBestEffort(OPERATION_SAVE_PREFS) {
            remoteDataSource.saveNotificationPrefs(
                userId = localStore.activeUserId,
                prefs = prefs,
            )
        }
        return KronoResult.Success(Unit)
    }

    private companion object {
        const val OPERATION_SAVE_PREFS = "guardar preferencias de notificación"
    }
}

