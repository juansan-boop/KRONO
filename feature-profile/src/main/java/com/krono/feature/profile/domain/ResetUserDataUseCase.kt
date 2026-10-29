package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso para restablecer la configuración y datos del usuario (F-21 CA-1).
 *
 * Borra las tareas (`users/{uid}/tasks`) y el cronómetro activo (`users/{uid}/timer/active`),
 * restaura los valores por defecto del perfil en Firestore y en el almacén local, y conserva
 * activa la cuenta. El borrado remoto es una única escritura atómica: nunca queda un estado parcial.
 *
 * Devuelve siempre un [KronoResult] para evitar excepciones no controladas.
 */
open class ResetUserDataUseCase @Inject constructor(
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
     * Ejecuta el restablecimiento integral de datos y configuración del usuario.
     *
     * Sin conexión falla de inmediato con [NetworkUnavailableException], sin emitir ninguna
     * escritura. Con conexión ejecuta la purga remota atómica y, solo si tuvo éxito, restaura el
     * almacén local; ante cualquier error no se limpia el almacén local ni se informa éxito.
     *
     * @return [KronoResult.Success] si la purga y la restauración culminaron, o el [KronoResult.Error] ocurrido.
     */
    open suspend operator fun invoke(): KronoResult<Unit> {
        if (!connectivityChecker.isOnline()) return KronoResult.Error(NetworkUnavailableException())

        val result = remoteDataSource.resetUserData(localStore.activeUserId)
        if (result is KronoResult.Error) return result

        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}
