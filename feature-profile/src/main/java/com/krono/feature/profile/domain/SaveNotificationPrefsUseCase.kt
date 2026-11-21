package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import javax.inject.Inject

/**
 * Caso de uso desacoplado dentro de `:feature-profile` para validar y persistir
 * las preferencias de notificaciones y alarmas (F-15 CA-2).
 *
 * Depende exclusivamente de [ProfileRepository], sin acoplamiento a fuentes de datos locales o remotas.
 */
open class SaveNotificationPrefsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

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
        return profileRepository.saveNotificationPrefs(prefs)
    }
}
