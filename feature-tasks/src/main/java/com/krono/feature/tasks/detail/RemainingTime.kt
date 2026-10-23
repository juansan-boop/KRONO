package com.krono.feature.tasks.detail

/**
 * Representación pura del tiempo restante de una tarea respecto al instante actual (F-06 CA-1).
 * Totalmente desacoplada de `Context` y Compose para garantizar pruebas deterministas en JVM.
 */
sealed interface RemainingTime {
    /** La fecha y hora límite ya expiraron. */
    data object Overdue : RemainingTime

    /** Quedan minutos para la entrega. */
    data class InMinutes(val minutes: Long) : RemainingTime

    /** Quedan horas para la entrega. */
    data class InHours(val hours: Long) : RemainingTime

    /** Quedan días para la entrega. */
    data class InDays(val days: Long) : RemainingTime
}
