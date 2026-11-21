package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import javax.inject.Inject

/**
 * Caso de uso para restablecer la configuración y datos del usuario (F-21 CA-1).
 *
 * Depende exclusivamente de [ProfileRepository], sin acoplamiento a fuentes de datos locales o remotas.
 */
open class ResetUserDataUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    /**
     * Ejecuta el restablecimiento integral de datos y configuración del usuario.
     *
     * @return [KronoResult.Success] si la purga y la restauración culminaron, o el [KronoResult.Error] ocurrido.
     */
    open suspend operator fun invoke(): KronoResult<Unit> {
        return profileRepository.resetUserData()
    }
}
