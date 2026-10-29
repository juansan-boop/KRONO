package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskRepository
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.NoOpTaskRepository
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso para restablecer la configuración y datos del usuario (F-21 CA-1).
 *
 * Borra todas las tareas del usuario en Cloud Firestore (`users/{uid}/tasks`) y su cronómetro
 * activo (`users/{uid}/timer/active`),
 * restaura los valores por defecto del perfil en Firestore (`users/{uid}`) y el almacén local,
 * conservando activa la cuenta del usuario.
 *
 * Devuelve siempre un [KronoResult] para evitar excepciones no controladas.
 */
open class ResetUserDataUseCase @Inject constructor(
    private val localStore: ProfileLocalStore,
    private val taskRepository: TaskRepository,
    private val remoteDataSource: ProfileRemoteDataSource,
) {

    constructor(localStore: ProfileLocalStore) : this(
        localStore = localStore,
        taskRepository = NoOpTaskRepository(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    constructor() : this(
        localStore = ProfileLocalStore(),
        taskRepository = NoOpTaskRepository(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
    )

    /**
     * Ejecuta el restablecimiento integral de datos y configuración del usuario.
     *
     * Orden: borrar tareas, borrar `timer/active`, restablecer el perfil remoto y, por último,
     * restaurar el almacén local. Ante el primer error se detiene y lo devuelve: no se limpia
     * el almacén local ni se informa éxito si algo falló.
     *
     * @return [KronoResult.Success] si la purga y restauración culminaron exitosamente,
     * o el primer [KronoResult.Error] ocurrido durante la operación.
     */
    open suspend operator fun invoke(): KronoResult<Unit> {
        val steps = listOf<suspend () -> KronoResult<Unit>>(
            { taskRepository.deleteAllTasks() },
            { remoteDataSource.clearActiveTimer(localStore.activeUserId) },
            { remoteDataSource.resetUserData(localStore.activeUserId) },
        )
        for (step in steps) {
            val result = step()
            if (result is KronoResult.Error) return result
        }

        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}
