package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import javax.inject.Inject

/**
 * Caso de uso para eliminar definitivamente la cuenta y el perfil del usuario (F-21 CA-2).
 *
 * Depende exclusivamente de [ProfileRepository], sin acoplamiento a fuentes de datos locales o remotas.
 */
open class DeleteAccountUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    /**
     * Reautentica y, solo si tiene éxito, borra los datos y elimina la cuenta.
     *
     * @param password Contraseña actual del usuario requerida para reautenticación sensible.
     * @return [KronoResult.Success] si la cuenta y los datos fueron eliminados, o [KronoResult.Error]
     * si la contraseña está en blanco ([BlankPasswordException]), no hay conexión o falla cualquier paso.
     */
    open suspend operator fun invoke(password: String): KronoResult<Unit> {
        if (password.isBlank()) {
            return KronoResult.Error(BlankPasswordException())
        }
        return profileRepository.deleteAccount(password)
    }
}
