package com.krono.core.domain.calendar

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** Inicio de este día (00:00), el límite inclusivo del día en las consultas del calendario. */
internal fun LocalDate.startOfDay(): LocalDateTime = LocalDateTime(date = this, time = LocalTime.fromSecondOfDay(0))
