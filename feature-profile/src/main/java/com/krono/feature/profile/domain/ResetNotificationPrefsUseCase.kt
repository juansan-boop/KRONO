package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import javax.inject.Inject

/**
 * Caso de uso desacoplado dentro de `:feature-profile` para restablecer las
 * preferencias de notificaciones y alarmas a sus valores predeterminados (F-15 CA-2).
 *
 * Depende exclusivamente de [ProfileRepository], sin acoplamiento a fuentes de datos locales o remotas.
 */
open class ResetNotificationPrefsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    /**
     * Restablece las preferencias a los valores predeterminados del sistema.
     *
     * @return [KronoResult.Success] conteniendo una nueva instancia de [NotificationPrefs]
     * con sus valores base de fábrica.
     */
    open suspend operator fun invoke(): KronoResult<NotificationPrefs> {
        return profileRepository.resetNotificationPrefs()
    }
}
