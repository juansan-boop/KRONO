package com.krono.core.data.task

import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.domain.task.TaskError

/**
 * Mapeador de excepciones de Cloud Firestore hacia errores de dominio [TaskError] (F-02 CA-3).
 */
object FirestoreTaskErrorMapper {

    /**
     * Traduce una excepción técnica a un error de dominio de tareas.
     *
     * @param throwable Excepción capturada.
     * @return Instancia correspondiente de [TaskError].
     */
    fun map(throwable: Throwable): TaskError = when (throwable) {
        is TaskError -> throwable
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.NOT_FOUND -> TaskError.TaskNotFound
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> TaskError.UnauthenticatedUser
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                TaskError.StorageError("Permiso denegado para acceder a las tareas.", throwable)
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                TaskError.StorageError("Servicio de base de datos no disponible temporalmente.", throwable)
            else -> TaskError.StorageError(throwable.message ?: "Error de Firestore", throwable)
        }
        else -> TaskError.StorageError(throwable.message ?: "Error desconocido en persistencia", throwable)
    }
}
