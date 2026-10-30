package com.krono.core.domain.focus

/** Errores de dominio de las plantillas de enfoque (F-10). */
sealed class FocusTemplateError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** El nombre está vacío o solo tiene espacios. */
    data object BlankName : FocusTemplateError("El nombre de la plantilla no puede estar vacío.")

    /** El nombre supera [FocusTemplateLimits.MAX_NAME_LENGTH] caracteres. */
    data object NameTooLong : FocusTemplateError("El nombre de la plantilla es demasiado largo.")

    /** Los minutos de foco no están entre el mínimo y [FocusTemplateLimits.MAX_FOCUS_MINUTES]. */
    data object InvalidFocusMinutes : FocusTemplateError("Los minutos de foco están fuera de rango.")

    /** Los minutos de descanso no están entre el mínimo y [FocusTemplateLimits.MAX_BREAK_MINUTES]. */
    data object InvalidBreakMinutes : FocusTemplateError("Los minutos de descanso están fuera de rango.")

    /** La plantilla indicada no existe. */
    data object TemplateNotFound : FocusTemplateError("La plantilla no existe.")

    /** No se puede eliminar la única plantilla: siempre debe quedar una activa. */
    data object CannotDeleteLastTemplate :
        FocusTemplateError("Debe quedar al menos una plantilla de enfoque.")

    /** No hay un usuario con sesión activa. */
    data object UnauthenticatedUser : FocusTemplateError("Se requiere una sesión activa.")

    /** Fallo de la persistencia (red, permisos o datos inválidos). */
    data class StorageError(val details: String, override val cause: Throwable? = null) :
        FocusTemplateError("Error al guardar las plantillas: $details", cause)
}
