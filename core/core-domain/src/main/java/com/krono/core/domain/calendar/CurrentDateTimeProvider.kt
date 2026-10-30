package com.krono.core.domain.calendar

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Fuente de "ahora" del calendario (F-16 CA-3): convierte el instante del [clock] a la hora
 * local, que es la que usan las fechas de las tareas. Inyectable para fijar la hora en pruebas.
 *
 * @param clock Reloj; en producción el del sistema.
 * @param timeZone Proveedor de la zona del usuario; se consulta en cada lectura para respetar
 * los cambios de zona del dispositivo.
 */
class CurrentDateTimeProvider(
    private val clock: Clock,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** Fecha y hora local actuales. */
    fun now(): LocalDateTime = clock.now().toLocalDateTime(timeZone())

    /** Fecha local actual (el botón "Hoy" y el día resaltado). */
    fun today(): LocalDate = now().date

    /** Instante en que empieza [date] (00:00) en la zona del usuario, para acotar consultas por día. */
    fun startOfDay(date: LocalDate): Instant = date.atStartOfDayIn(timeZone())
}
