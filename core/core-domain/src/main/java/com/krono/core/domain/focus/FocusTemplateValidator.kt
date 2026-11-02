package com.krono.core.domain.focus

/** Resultado de validar los datos de una plantilla: un error opcional por campo. */
data class FocusTemplateValidation(
    val nameError: FocusTemplateError? = null,
    val focusError: FocusTemplateError? = null,
    val breakError: FocusTemplateError? = null,
) {
    /** `true` si ningún campo tiene error. */
    val isValid: Boolean get() = nameError == null && focusError == null && breakError == null

    /** Primer error encontrado, en el orden nombre, foco, descanso; `null` si es válido. */
    val firstError: FocusTemplateError? get() = nameError ?: focusError ?: breakError
}

/** Valida los datos de una plantilla de enfoque (F-10 CA-2) con los [FocusTemplateLimits]. */
object FocusTemplateValidator {

    /**
     * Valida el nombre (no vacío, con longitud máxima) y los minutos de foco y descanso
     * (mayores que cero y bajo su máximo).
     */
    fun validate(name: String, focusMinutes: Int, breakMinutes: Int): FocusTemplateValidation =
        FocusTemplateValidation(
            nameError = nameError(name.trim()),
            focusError = FocusTemplateError.InvalidFocusMinutes.takeUnless {
                focusMinutes in FocusTemplateLimits.MIN_MINUTES..FocusTemplateLimits.MAX_FOCUS_MINUTES
            },
            breakError = FocusTemplateError.InvalidBreakMinutes.takeUnless {
                breakMinutes in FocusTemplateLimits.MIN_MINUTES..FocusTemplateLimits.MAX_BREAK_MINUTES
            },
        )

    private fun nameError(trimmedName: String): FocusTemplateError? = when {
        trimmedName.isEmpty() -> FocusTemplateError.BlankName
        trimmedName.length > FocusTemplateLimits.MAX_NAME_LENGTH -> FocusTemplateError.NameTooLong
        else -> null
    }
}
