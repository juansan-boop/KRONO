package com.krono.feature.tasks.edit

/**
 * Eventos unidireccionales emitidos por [EditTaskViewModel] hacia la UI (F-05).
 */
sealed interface EditTaskEvent {
    /** Navega de vuelta tras guardar los cambios exitosamente o al cancelar (CA-2). */
    data object NavigateBack : EditTaskEvent

    /** Muestra un mensaje temporal en la interfaz (snackbar). */
    data class ShowSnackbar(val message: String) : EditTaskEvent
}
