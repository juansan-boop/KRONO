package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskRepository
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.NoOpTaskRepository
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso para eliminar definitivamente la cuenta y el perfil del usuario (F-21 CA-2).
 *
 * Borra permanentemente las tareas del usuario en Cloud Firestore (`users/{uid}/tasks`),
 * elimina el documento de perfil `users/{uid}`, reautentica con la contraseña provista
 * y da de baja la cuenta en Firebase Authentication.
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
     * Ejecuta la reautenticación y posterior eliminación de la cuenta y sus datos en Firestore.
     *
     * @param password Contraseña actual del usuario requerida para reautenticación sensible.
     * @return [KronoResult.Success] si la cuenta y datos fueron eliminados,
     * o [KronoResult.Error] si la contraseña está en blanco o falla la autenticación/borrado.
     */
    open suspend operator fun invoke(password: String): KronoResult<Unit> {
        if (password.isBlank()) {
            return KronoResult.Error(IllegalArgumentException("La contraseña de reautenticación no puede estar vacía"))
        }

        // 1. Borrar todas las tareas en users/{uid}/tasks
        try {
            val tasks = taskRepository.observeTasks().first()
            for (task in tasks) {
                taskRepository.deleteTask(task.id)
            }
        } catch (_: Throwable) {
            // Continúa para no bloquear el borrado de la cuenta ante errores de red secundarios
        }

        // 2. Reautenticar, borrar documento users/{uid} y eliminar usuario de Firebase Auth
        val remoteResult = remoteDataSource.deleteAccount(password)
        if (remoteResult is KronoResult.Error) {
            return remoteResult
        }

        // 3. Limpiar almacenamiento local
        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}

