package com.krono.core.data.timer

import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FocusBlock
import com.krono.core.domain.timer.FocusBreak
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionType
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serialización del cronómetro en curso para Cloud Firestore (F-08). Las marcas de tiempo
 * se guardan en milisegundos desde la época Unix, igual que las duraciones de `TaskDto`.
 *
 * Un enfoque (F-09) añade los campos `focus*` y `break*` al mismo documento; un cronómetro simple
 * no los escribe, y un documento anterior al modo enfoque se lee como cronómetro.
 */
object ActiveTimerDto {
    const val FIELD_TASK_ID = "taskId"
    const val FIELD_STARTED_AT_MILLIS = "startedAtMillis"
    const val FIELD_RUNNING_SINCE_MILLIS = "runningSinceMillis"
    const val FIELD_ACCUMULATED_MILLIS = "accumulatedMillis"
    const val FIELD_FOCUS_BLOCK_MILLIS = "focusBlockMillis"
    const val FIELD_FOCUS_BREAK_MILLIS = "focusBreakMillis"
    const val FIELD_FOCUS_TARGET_MILLIS = "focusTargetMillis"
    const val FIELD_BREAK_STARTED_AT_MILLIS = "breakStartedAtMillis"
    const val FIELD_BREAK_DURATION_MILLIS = "breakDurationMillis"

    /** Convierte [timer] en un mapa para Firestore. */
    fun toMap(timer: ActiveTimer): Map<String, Any?> = mapOf(
        FIELD_TASK_ID to timer.taskId,
        FIELD_STARTED_AT_MILLIS to timer.startedAt.toEpochMilliseconds(),
        FIELD_RUNNING_SINCE_MILLIS to timer.runningSince?.toEpochMilliseconds(),
        FIELD_ACCUMULATED_MILLIS to timer.accumulated.inWholeMilliseconds,
    ) + (timer.focus?.let(::focusToMap) ?: emptyMap())

    private fun focusToMap(focus: FocusBlock): Map<String, Any?> = mapOf(
        FIELD_FOCUS_BLOCK_MILLIS to focus.blockDuration.inWholeMilliseconds,
        FIELD_FOCUS_BREAK_MILLIS to focus.breakDuration.inWholeMilliseconds,
        FIELD_FOCUS_TARGET_MILLIS to focus.targetElapsed.inWholeMilliseconds,
        FIELD_BREAK_STARTED_AT_MILLIS to focus.breakInProgress?.startedAt?.toEpochMilliseconds(),
        FIELD_BREAK_DURATION_MILLIS to focus.breakInProgress?.duration?.inWholeMilliseconds,
    )

    /** Reconstruye el cronómetro desde un documento; `null` si le faltan datos obligatorios. */
    fun fromMap(map: Map<String, Any?>): ActiveTimer? {
        val taskId = map[FIELD_TASK_ID] as? String ?: return null
        val startedAt = (map[FIELD_STARTED_AT_MILLIS] as? Number)?.toLong() ?: return null
        return ActiveTimer(
            taskId = taskId,
            startedAt = Instant.fromEpochMilliseconds(startedAt),
            runningSince = (map[FIELD_RUNNING_SINCE_MILLIS] as? Number)?.toLong()?.let(Instant::fromEpochMilliseconds),
            accumulated = ((map[FIELD_ACCUMULATED_MILLIS] as? Number)?.toLong() ?: 0L).milliseconds,
            focus = focusFromMap(map),
        )
    }

    /** Lee el enfoque; `null` (cronómetro simple) si falta alguno de sus campos obligatorios. */
    private fun focusFromMap(map: Map<String, Any?>): FocusBlock? {
        val block = (map[FIELD_FOCUS_BLOCK_MILLIS] as? Number)?.toLong() ?: return null
        val breakLength = (map[FIELD_FOCUS_BREAK_MILLIS] as? Number)?.toLong() ?: return null
        val target = (map[FIELD_FOCUS_TARGET_MILLIS] as? Number)?.toLong() ?: return null
        val breakStartedAt = (map[FIELD_BREAK_STARTED_AT_MILLIS] as? Number)?.toLong()
        val breakDuration = (map[FIELD_BREAK_DURATION_MILLIS] as? Number)?.toLong()
        return FocusBlock(
            blockDuration = block.milliseconds,
            breakDuration = breakLength.milliseconds,
            targetElapsed = target.milliseconds,
            breakInProgress = if (breakStartedAt != null && breakDuration != null) {
                FocusBreak(Instant.fromEpochMilliseconds(breakStartedAt), breakDuration.milliseconds)
            } else {
                null
            },
        )
    }
}

/** Serialización de una sesión de tiempo para Cloud Firestore (Sección 6 del SPEC). */
object TimeSessionDto {
    const val FIELD_ID = "id"
    const val FIELD_TASK_ID = "taskId"
    const val FIELD_STARTED_AT_MILLIS = "startedAtMillis"
    const val FIELD_ENDED_AT_MILLIS = "endedAtMillis"
    const val FIELD_DURATION_MILLIS = "durationMillis"
    const val FIELD_TYPE = "type"

    /** Convierte [session] en un mapa para Firestore. */
    fun toMap(session: TimeSession): Map<String, Any?> = mapOf(
        FIELD_ID to session.id,
        FIELD_TASK_ID to session.taskId,
        FIELD_STARTED_AT_MILLIS to session.startedAt.toEpochMilliseconds(),
        FIELD_ENDED_AT_MILLIS to session.endedAt.toEpochMilliseconds(),
        FIELD_DURATION_MILLIS to session.duration.inWholeMilliseconds,
        FIELD_TYPE to session.type.name,
    )

    /**
     * Reconstruye la sesión desde un documento; `null` si le faltan datos obligatorios o su tipo
     * es desconocido, para que un documento dañado no rompa toda la lectura.
     */
    fun fromMap(map: Map<String, Any?>): TimeSession? {
        val id = map[FIELD_ID] as? String ?: return null
        val taskId = map[FIELD_TASK_ID] as? String ?: return null
        val startedAt = (map[FIELD_STARTED_AT_MILLIS] as? Number)?.toLong() ?: return null
        val endedAt = (map[FIELD_ENDED_AT_MILLIS] as? Number)?.toLong() ?: return null
        val durationMillis = (map[FIELD_DURATION_MILLIS] as? Number)?.toLong() ?: return null
        val type = TimeSessionType.entries.firstOrNull { it.name == map[FIELD_TYPE] } ?: return null
        return TimeSession(
            id = id,
            taskId = taskId,
            startedAt = Instant.fromEpochMilliseconds(startedAt),
            endedAt = Instant.fromEpochMilliseconds(endedAt),
            duration = durationMillis.milliseconds,
            type = type,
        )
    }
}
