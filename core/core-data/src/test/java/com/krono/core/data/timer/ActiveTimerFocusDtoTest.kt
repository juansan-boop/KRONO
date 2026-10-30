package com.krono.core.data.timer

import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FocusBlock
import com.krono.core.domain.timer.FocusBreak
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de la serialización del enfoque dentro del documento del cronómetro (F-09). */
class ActiveTimerFocusDtoTest {

    private val start = Instant.parse("2026-10-30T09:00:00Z")
    private val focusTimer = ActiveTimer(
        taskId = "tarea-1",
        startedAt = start,
        runningSince = null,
        accumulated = 12.minutes,
        focus = FocusBlock(
            blockDuration = 45.minutes,
            breakDuration = 10.minutes,
            targetElapsed = 90.minutes,
            breakInProgress = FocusBreak(start + 12.minutes, 5.minutes),
        ),
    )

    @Test
    fun unEnfoqueConDescanso_sobreviveAlViajeDeIdaYVuelta() {
        assertEquals(focusTimer, ActiveTimerDto.fromMap(ActiveTimerDto.toMap(focusTimer)))
    }

    @Test
    fun unEnfoqueSinDescanso_sobreviveAlViajeDeIdaYVuelta() {
        val timer = focusTimer.copy(focus = focusTimer.focus!!.copy(breakInProgress = null))

        assertEquals(timer, ActiveTimerDto.fromMap(ActiveTimerDto.toMap(timer)))
    }

    @Test
    fun unCronometroSimpleNoEscribeCamposDeEnfoque() {
        val map = ActiveTimerDto.toMap(focusTimer.copy(focus = null))

        assertFalse(map.containsKey(ActiveTimerDto.FIELD_FOCUS_BLOCK_MILLIS))
        assertNull(ActiveTimerDto.fromMap(map)!!.focus)
    }

    @Test
    fun unDocumentoDeCronometroAnterior_seLeeSinEnfoque() {
        val legacy = mapOf<String, Any?>(
            ActiveTimerDto.FIELD_TASK_ID to "tarea-1",
            ActiveTimerDto.FIELD_STARTED_AT_MILLIS to start.toEpochMilliseconds(),
            ActiveTimerDto.FIELD_RUNNING_SINCE_MILLIS to start.toEpochMilliseconds(),
            ActiveTimerDto.FIELD_ACCUMULATED_MILLIS to 0L,
        )

        assertNull(ActiveTimerDto.fromMap(legacy)!!.focus)
    }

    @Test
    fun unEnfoqueConCamposIncompletos_seLeeComoCronometro() {
        val map = ActiveTimerDto.toMap(focusTimer) - ActiveTimerDto.FIELD_FOCUS_TARGET_MILLIS

        assertNull(ActiveTimerDto.fromMap(map)!!.focus)
    }
}
