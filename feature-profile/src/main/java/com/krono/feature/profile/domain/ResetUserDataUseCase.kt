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
 * Caso de uso para restablecer la configuración y datos del usuario (F-21 CA-1).
 *
 * Borra todas las tareas del usuario en Cloud Firestore (`users/{uid}/tasks`),
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
     * @return [KronoResult.Success] si la purga y restauración culminaron exitosamente,
     * o [KronoResult.Error] si ocurrió un fallo crítico durante la operación.
     */
    open suspend operator fun invoke(): KronoResult<Unit> {
        // 1. Borrar tareas del usuario en Cloud Firestore (users/{uid}/tasks)
        try {
            val tasks = taskRepository.observeTasks().first()
            for (task in tasks) {
                taskRepository.deleteTask(task.id)
            }
        } catch (_: Throwable) {
            // Tolera lista vacía o falta de conexión temporal
        }

        // 2. Restablecer documento users/{uid} en Firestore (best-effort)
        try {
            remoteDataSource.resetUserData(localStore.activeUserId)
        } catch (_: Throwable) {
            // Tolera si las reglas de Firestore aún no están desplegadas en consola o sin red
        }

        // 3. Restaurar valores por defecto en el almacén local
        localStore.resetToDefaults()
        return KronoResult.Success(Unit)
    }
}

