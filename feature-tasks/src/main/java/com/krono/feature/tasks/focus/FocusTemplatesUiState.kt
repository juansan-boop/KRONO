package com.krono.feature.tasks.focus

import androidx.annotation.StringRes
import com.krono.core.domain.focus.FocusTemplate

/**
 * Estado inmutable de la pantalla de plantillas de enfoque (F-10). Sin imports de `androidx.compose.*`.
 *
 * @property isLoading `true` mientras llega la primera lectura de plantillas.
 * @property hasLoadError `true` si la lectura falló; la pantalla ofrece reintentar en vez de fingir una lista vacía.
 * @property templates plantillas con la activa primero (CA-1, CA-3).
 * @property editor formulario de crear o editar abierto, o `null` si no hay ninguno.
 * @property templatePendingDelete plantilla cuya eliminación espera confirmación, o `null`.
 * @property messageRes mensaje transitorio de un error de escritura, o `null`.
 */
data class FocusTemplatesUiState(
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    val templates: List<FocusTemplate> = emptyList(),
    val editor: FocusTemplateEditorState? = null,
    val templatePendingDelete: FocusTemplate? = null,
    @StringRes val messageRes: Int? = null,
) {
    /** Solo se puede eliminar si queda otra plantilla: siempre debe haber una activa (CA-2). */
    val canDelete: Boolean get() = templates.size > 1
}

/**
 * Datos del formulario de una plantilla. Los minutos se guardan como texto para conservar lo que
 * el usuario escribe, incluido el campo vacío.
 *
 * @property templateId id de la plantilla que se edita, o `null` si se está creando.
 * @property name nombre escrito.
 * @property focusMinutesText minutos de foco escritos.
 * @property breakMinutesText minutos de descanso escritos.
 * @property nameErrorRes error de validación del nombre.
 * @property focusErrorRes error de validación de los minutos de foco.
 * @property breakErrorRes error de validación de los minutos de descanso.
 * @property isSaving `true` mientras se guarda, para ignorar toques repetidos.
 */
data class FocusTemplateEditorState(
    val templateId: String? = null,
    val name: String = "",
    val focusMinutesText: String = "",
    val breakMinutesText: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val focusErrorRes: Int? = null,
    @StringRes val breakErrorRes: Int? = null,
    val isSaving: Boolean = false,
) {
    /** `true` si se edita una plantilla existente; `false` si se crea una nueva. */
    val isEditing: Boolean get() = templateId != null
}
