package com.krono.core.domain.focus

/**
 * Plantilla de bloque de enfoque (F-10): cuántos minutos se trabaja y cuántos se descansa en un
 * ciclo. Solo una plantilla está activa a la vez; es la que usa el modo enfoque (F-09).
 *
 * @property id identificador estable del documento.
 * @property name nombre visible, por ejemplo "Estudio Profundo".
 * @property focusMinutes minutos de trabajo del ciclo.
 * @property breakMinutes minutos de descanso del ciclo.
 * @property isActive `true` si es la plantilla en uso.
 */
data class FocusTemplate(
    val id: String,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val isActive: Boolean,
) {
    /** Duración total del ciclo, en minutos. */
    val cycleMinutes: Int get() = focusMinutes + breakMinutes

    /** Porcentaje entero del ciclo dedicado al foco (F-10 CA-3), redondeado al más cercano. */
    val focusPercent: Int
        get() = (focusMinutes * PERCENT_TOTAL + cycleMinutes / 2) / cycleMinutes

    /** Porcentaje entero del ciclo dedicado al descanso; con [focusPercent] siempre suma 100. */
    val breakPercent: Int get() = PERCENT_TOTAL - focusPercent

    private companion object {
        const val PERCENT_TOTAL = 100
    }
}
