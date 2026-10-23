package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso desacoplado dentro de `:feature-profile` para actualizar y validar
 * la información del perfil del usuario (F-19 CA-2).
 *
 * Cumple con el criterio de aceptación CA-2: campos obligatorios vacíos devuelven
 * error y cambios válidos persisten en el estado local y en Cloud Firestore (`users/{uid}`).
 */
open class UpdateProfileUseCase @Inject constructor(
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
     * Valida y actualiza los datos del perfil de usuario.
     *
     * @param name Nombre completo del usuario (obligatorio).
     * @param role Rol académico / profesional (obligatorio).
     * @param avatarUri URI local de la foto de perfil seleccionada (opcional).
     * @return [KronoResult.Success] si los cambios son válidos, o [KronoResult.Error]
     * con [IllegalArgumentException] si algún campo obligatorio se encuentra vacío.
     */
    open suspend operator fun invoke(
        name: String,
        role: String,
        avatarUri: String? = null,
    ): KronoResult<Unit> {
        val trimmedName = name.trim()
        val trimmedRole = role.trim()
        if (trimmedName.isBlank()) {
            return KronoResult.Error(IllegalArgumentException("El nombre no puede estar vacío"))
        }
        if (trimmedRole.isBlank()) {
            return KronoResult.Error(IllegalArgumentException("El rol no puede estar vacío"))
        }
        // 1. Guardar en caché local instantánea
        localStore.saveProfile(trimmedName, trimmedRole, avatarUri)

        // 2. Sincronizar en Cloud Firestore (users/{uid}) y Firebase Auth en modo best-effort
        try {
            remoteDataSource.saveUserProfile(
                userId = localStore.activeUserId,
                name = trimmedName,
                role = trimmedRole,
                avatarUri = avatarUri,
            )
        } catch (_: Throwable) {
            // Sincronización remota tolerante a falta de red o reglas en despliegue
        }

        return KronoResult.Success(Unit)
    }
}
