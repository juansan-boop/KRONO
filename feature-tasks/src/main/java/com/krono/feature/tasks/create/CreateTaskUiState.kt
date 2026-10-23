package com.krono.feature.tasks.create

import androidx.annotation.StringRes
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/**
 * Estado inmutable de la pantalla de creación de tarea (F-04).
 *
 * @property title Título o nombre de la tarea en edición.
 * @property category Categoría funcional seleccionada (Estudio, Trabajo, Personal).
 * @property dueDate Fecha límite seleccionada, o `null` si aún no se define.
 * @property dueTime Hora límite seleccionada, o `null` si aún no se define.
 * @property priority Nivel de prioridad asignado (Baja, Media, Alta).
 * @property estimatedDuration Duración estimada opcional (CA-4).
 * @property durationSuggestion Duración sugerida por el historial de la categoría (F-11), o
 * `null` si no hay historial suficiente.
 * @property titleErrorRes Recurso de texto de error de validación de título (CA-2).
 * @property dateErrorRes Recurso de texto de error cuando falta la fecha (CA-2).
 * @property dateTimeErrorRes Recurso de texto cuando la fecha/hora es en el pasado (CA-3).
 * @property isSaving Indica si se está ejecutando la persistencia.
 * @property errorMessage Mensaje de error general de la operación si falla Firestore.
 */
data class CreateTaskUiState(
    val title: String = "",
    val category: TaskCategory = TaskCategory.ESTUDIO,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.MEDIA,
    val estimatedDuration: Duration? = null,
    val durationSuggestion: Duration? = null,
    @StringRes val titleErrorRes: Int? = null,
    @StringRes val dateErrorRes: Int? = null,
    @StringRes val dateTimeErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Indica si hay un error visual asociado al campo del título. */
    val isTitleError: Boolean get() = titleErrorRes != null

    /** Indica si hay un error visual asociado al selector de fecha. */
    val isDateError: Boolean get() = dateErrorRes != null || dateTimeErrorRes != null

    /** Indica si hay un error visual asociado al selector de hora. */
    val isTimeError: Boolean get() = dateTimeErrorRes != null

    /** Indica si se muestra la sugerencia: existe y aún no se ha aplicado (F-11 CA-1). */
    val showDurationSuggestion: Boolean
        get() = durationSuggestion != null && durationSuggestion != estimatedDuration

    /** Permite enviar el formulario si no hay un guardado en curso. */
    val canSubmit: Boolean get() = !isSaving
}
