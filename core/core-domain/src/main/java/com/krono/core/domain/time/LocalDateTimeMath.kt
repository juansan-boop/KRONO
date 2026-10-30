package com.krono.core.domain.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

// Las fechas de las tareas son locales y sin zona. Para sumar y restar se interpretan en UTC:
// así la aritmética no depende de la zona del dispositivo. Se ignoran los cambios de horario
// de verano, un desfase aceptable para el asistente de prioridades y el calendario.

/** Suma [duration] a esta fecha y hora local. */
internal operator fun LocalDateTime.plus(duration: Duration): LocalDateTime =
    toInstant(TimeZone.UTC).plus(duration).toLocalDateTime(TimeZone.UTC)

/** Tiempo que va desde esta fecha y hora local hasta [other]; negativo si [other] es anterior. */
internal fun LocalDateTime.durationUntil(other: LocalDateTime): Duration =
    other.toInstant(TimeZone.UTC) - toInstant(TimeZone.UTC)
