package com.krono.core.domain.calendar

import com.krono.core.domain.time.plus
import com.krono.core.domain.task.Task
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Duración del bloque de una tarea sin estimación válida (D-14). */
val DEFAULT_BLOCK_DURATION: Duration = 30.minutes

/**
 * Deriva el bloque de calendario de esta tarea (D-14): termina en su fecha límite y empieza
 * `estimatedDuration` antes. Una estimación ausente, cero o negativa se trata como "sin
 * estimación" y ocupa [DEFAULT_BLOCK_DURATION].
 *
 * @return el bloque, o `null` si la tarea no tiene fecha límite (no aparece en el calendario).
 */
fun Task.toCalendarBlock(): CalendarBlock? {
    val end = dueDate ?: return null
    val estimate = estimatedDuration?.takeIf { it > Duration.ZERO }
    val duration = estimate ?: DEFAULT_BLOCK_DURATION
    val source = if (estimate != null) BlockDurationSource.ESTIMATED else BlockDurationSource.DEFAULT
    return CalendarBlock(task = this, start = end.plus(-duration), end = end, durationSource = source)
}
