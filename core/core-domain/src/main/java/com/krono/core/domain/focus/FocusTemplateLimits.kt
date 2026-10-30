package com.krono.core.domain.focus

/** Límites de los datos de una plantilla de enfoque (F-10). */
object FocusTemplateLimits {
    /** Longitud máxima del nombre, en caracteres. */
    const val MAX_NAME_LENGTH = 40

    /** Mínimo de minutos de foco o de descanso: debe ser mayor que cero. */
    const val MIN_MINUTES = 1

    /** Máximo de minutos de foco de un ciclo (3 horas). */
    const val MAX_FOCUS_MINUTES = 180

    /** Máximo de minutos de descanso de un ciclo. */
    const val MAX_BREAK_MINUTES = 60
}
