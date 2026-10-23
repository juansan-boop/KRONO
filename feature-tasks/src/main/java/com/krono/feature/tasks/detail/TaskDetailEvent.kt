package com.krono.feature.tasks.detail

/**
 * Eventos unidireccionales emitidos desde [TaskDetailViewModel] hacia la UI (F-06).
 */
sealed interface TaskDetailEvent {
    /** Navega hacia atrás (volver a la lista de tareas). */
    data object NavigateBack : TaskDetailEvent

    /** Navega a la pantalla de edición de la tarea (F-05). */
    data class NavigateToEdit(val taskId: String) : TaskDetailEvent

    /** Navega al modo enfoque para la tarea (F-09 CA-3). */
    data class NavigateToFocus(val taskId: String) : TaskDetailEvent

    /** Muestra un mensaje temporal en la interfaz (snackbar). */
    data class ShowSnackbar(val message: String) : TaskDetailEvent
}
