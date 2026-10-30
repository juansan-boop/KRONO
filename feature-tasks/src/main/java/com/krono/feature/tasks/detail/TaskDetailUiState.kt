package com.krono.feature.tasks.detail

import com.krono.core.domain.task.Task

/**
 * Estado inmutable de la pantalla de detalle de tarea y subtareas (F-06).
 *
 * Cumple con la regla dura de arquitectura: cero imports de `androidx.compose.*`.
 *
 * @property task Tarea actualmente visualizada, o `null` si no ha cargado.
 * @property remainingTime Cálculo determinista del tiempo restante para entrega (CA-1).
 * @property isLoading Indica si se está consultando la tarea en el repositorio.
 * @property isTaskNotFound Indica si la tarea no existe en la persistencia.
 * @property isAddingSubtask Indica si se muestra el diálogo para añadir una nueva subtarea.
 * @property isSavingSubtask Indica si se está persistiendo una actualización de subtareas.
 * @property isConfirmingDelete Indica si se muestra el diálogo de confirmación de eliminación (F-07).
 * @property errorMessage Mensaje de error general si falla una operación.
 * @property hasLoadError Indica que no se pudo leer la tarea (por ejemplo, permiso denegado), lo
 * que es distinto de que la tarea no exista; la pantalla ofrece reintentar.
 */
data class TaskDetailUiState(
    val task: Task? = null,
    val remainingTime: RemainingTime? = null,
    val isLoading: Boolean = true,
    val isTaskNotFound: Boolean = false,
    val isAddingSubtask: Boolean = false,
    val isSavingSubtask: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadError: Boolean = false,
) {
    /** Cantidad total de subtareas embebidas. */
    val totalSubtasksCount: Int get() = task?.subtasks?.size ?: 0

    /** Cantidad de subtareas completadas (`isDone == true`). */
    val completedSubtasksCount: Int get() = task?.subtasks?.count { it.isDone } ?: 0

    /**
     * Razón de progreso entre 0f y 1f para la barra visual (CA-1, CA-2).
     */
    val progressRatio: Float get() = if (totalSubtasksCount > 0) {
        completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()
    } else {
        0f
    }
}
