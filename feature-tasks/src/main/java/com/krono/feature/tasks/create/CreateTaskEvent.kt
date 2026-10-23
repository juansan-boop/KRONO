package com.krono.feature.tasks.create

/**
 * Eventos unidireccionales emitidos por [CreateTaskViewModel] hacia la UI (F-04).
 */
sealed interface CreateTaskEvent {
    /** Navega de vuelta a la pantalla de lista de tareas tras un guardado exitoso (CA-1). */
    data object NavigateBack : CreateTaskEvent

    /** Muestra un mensaje temporal en la interfaz (snackbar). */
    data class ShowSnackbar(val message: String) : CreateTaskEvent
}
