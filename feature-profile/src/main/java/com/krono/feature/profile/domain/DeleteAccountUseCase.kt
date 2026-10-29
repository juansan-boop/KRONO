package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskRepository
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.NoOpTaskRepository
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import javax.inject.Inject

/**
 * Caso de uso para eliminar definitivamente la cuenta y el perfil del usuario (F-21 CA-2).
 *
 * Reautentica con la contraseña provista y, solo entonces, borra permanentemente las tareas
 * (`users/{uid}/tasks`) y el cronómetro activo (`users/{uid}/timer/active`), elimina el documento
 * de perfil `users/{uid}` y da de baja la cuenta en Firebase Authentication.
 *
 * Devuelve siempre un [KronoResult] para evitar excepciones no controladas.
 */
open class DeleteAccountUseCase @Inject constructor(
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
     * Reautentica y, solo si tiene éxito, borra tareas y cronómetro activo y elimina la cuenta.
     *
     * Orden: reautenticar, borrar tareas, borrar `timer/active`, eliminar perfil y cuenta de
     * Firebase Authentication y, por último, limpiar el almacén local. Ante el primer error se
     * detiene y lo devuelve sin ejecutar los pasos siguientes.
     *
     * @param password Contraseña actual del usuario requerida para reautenticación sensible.
     * @return [KronoResult.Success] si la cuenta y los datos fueron eliminados,
     * o [KronoResult.Error] si la contraseña está en blanco o falla cualquier paso.
     */
    open suspend operator fun invoke(password: String): KronoResult<Unit> {
        if (password.isBlank()) {
            return KronoResult.Error(IllegalArgumentException("La contraseña de reautenticación no puede estar vacía"))
        }

        val steps = listOf<suspend () -> KronoResult<Unit>>(
            { remoteDataSource.reauthenticate(password) },
            { taskRepository.deleteAllTasks() },
            { remoteDataSource.clearActiveTimer(localStore.activeUserId) },
            { remoteDataSource.deleteAccount() },
        )
        for (step in steps) {
            val result = step()
            if (result is KronoResult.Error) return result
        }

        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}
