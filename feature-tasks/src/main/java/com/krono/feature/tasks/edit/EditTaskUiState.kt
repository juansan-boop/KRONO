package com.krono.feature.tasks.edit

import androidx.annotation.StringRes
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/**
 * Estado inmutable de la pantalla de edición de tarea (F-05).
 *
 * Sigue estrictamente la regla de arquitectura: sin imports de `androidx.compose.*`.
 *
 * @property taskId Identificador de la tarea que se está editando.
 * @property title Título o nombre de la tarea en edición.
 * @property category Categoría funcional de la tarea (Estudio, Trabajo, Personal).
 * @property dueDate Fecha límite de entrega seleccionada, o `null` si no tiene.
 * @property dueTime Hora límite de entrega seleccionada, o `null` si no tiene.
 * @property priority Nivel de prioridad asignado (Baja, Media, Alta).
 * @property estimatedDuration Duración estimada opcional.
 * @property titleErrorRes Recurso de texto de error de validación de título (CA-3).
 * @property dateErrorRes Recurso de texto de error cuando falta la fecha (CA-3).
 * @property dateTimeErrorRes Recurso de texto cuando la fecha/hora es en el pasado (CA-3).
 * @property isLoading Indica si se está cargando la tarea desde la base de datos (CA-1).
 * @property isTaskNotFound Indica si la tarea no fue encontrada por su identificador.
 * @property isSaving Indica si se está ejecutando la persistencia de cambios (CA-2).
 * @property errorMessage Mensaje de error general de la operación si falla Firestore o la consulta.
 */
data class EditTaskUiState(
    val taskId: String = "",
    val title: String = "",
    val category: TaskCategory = TaskCategory.ESTUDIO,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.MEDIA,
    val estimatedDuration: Duration? = null,
    @StringRes val titleErrorRes: Int? = null,
    @StringRes val dateErrorRes: Int? = null,
    @StringRes val dateTimeErrorRes: Int? = null,
    val isLoading: Boolean = true,
    val isTaskNotFound: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Indica si hay un error visual asociado al campo del título. */
    val isTitleError: Boolean get() = titleErrorRes != null

    /** Indica si hay un error visual asociado al selector de fecha. */
    val isDateError: Boolean get() = dateErrorRes != null || dateTimeErrorRes != null

    /** Indica si hay un error visual asociado al selector de hora. */
    val isTimeError: Boolean get() = dateTimeErrorRes != null

    /** Determina si el formulario puede ser enviado válidamente. */
    val canSubmit: Boolean get() = !isLoading && !isTaskNotFound && !isSaving
}
