package com.krono.core.domain.task

/**
 * Errores de dominio asociados a la gestión y persistencia de tareas (F-02 CA-3).
 */
sealed class TaskError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** La tarea solicitada no existe o no fue encontrada en Firestore. */
    data object TaskNotFound : TaskError("La tarea solicitada no existe o no fue encontrada.")

    /** No existe un usuario autenticado activo para asociar las tareas. */
    data object UnauthenticatedUser : TaskError("Se requiere una sesión activa para gestionar tareas.")

    /** Error de validación en los datos de la tarea antes de persistir. */
    data class ValidationError(val reason: String) : TaskError(reason)

    /** Error durante la comunicación con Firestore o persistencia local. */
    data class StorageError(val details: String, override val cause: Throwable? = null) :
        TaskError("Error al persistir la tarea: $details", cause)
}
