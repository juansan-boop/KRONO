package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso para eliminar definitivamente la cuenta y el perfil del usuario (F-21 CA-2).
 *
 * Reautentica con la contraseña provista y, solo entonces, borra de forma atómica las tareas
 * (`users/{uid}/tasks`), el cronómetro activo (`users/{uid}/timer/active`) y el documento de
 * perfil `users/{uid}`, y da de baja la cuenta en Firebase Authentication.
 *
 * Devuelve siempre un [KronoResult] para evitar excepciones no controladas.
 */
open class DeleteAccountUseCase @Inject constructor(
    private val localStore: ProfileLocalStore,
    private val remoteDataSource: ProfileRemoteDataSource,
    private val connectivityChecker: ConnectivityChecker,
) {

    constructor(localStore: ProfileLocalStore) : this(
        localStore = localStore,
        remoteDataSource = NoOpProfileRemoteDataSource(),
        connectivityChecker = { true },
    )

    constructor() : this(ProfileLocalStore())

    /**
     * Reautentica y, solo si tiene éxito, borra los datos y elimina la cuenta.
     *
     * Orden: comprobar conexión, reautenticar, borrar datos y cuenta y, por último, limpiar el
     * almacén local. Ante el primer error se detiene y lo devuelve sin ejecutar los pasos siguientes.
     *
     * @param password Contraseña actual del usuario requerida para reautenticación sensible.
     * @return [KronoResult.Success] si la cuenta y los datos fueron eliminados, o [KronoResult.Error]
     * si la contraseña está en blanco, no hay conexión o falla cualquier paso.
     */
    open suspend operator fun invoke(password: String): KronoResult<Unit> {
        if (password.isBlank()) {
            return KronoResult.Error(IllegalArgumentException("La contraseña de reautenticación no puede estar vacía"))
        }
        if (!connectivityChecker.isOnline()) return KronoResult.Error(NetworkUnavailableException())

        val reauthentication = remoteDataSource.reauthenticate(password)
        if (reauthentication is KronoResult.Error) return reauthentication

        val deletion = remoteDataSource.deleteAccount(localStore.activeUserId)
        if (deletion is KronoResult.Error) return deletion

        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}
