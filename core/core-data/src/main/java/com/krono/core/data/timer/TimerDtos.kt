package com.krono.core.data.timer

import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.TimeSession
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serialización del cronómetro en curso para Cloud Firestore (F-08). Las marcas de tiempo
 * se guardan en milisegundos desde la época Unix, igual que las duraciones de `TaskDto`.
 */
object ActiveTimerDto {
    const val FIELD_TASK_ID = "taskId"
    const val FIELD_STARTED_AT_MILLIS = "startedAtMillis"
    const val FIELD_RUNNING_SINCE_MILLIS = "runningSinceMillis"
    const val FIELD_ACCUMULATED_MILLIS = "accumulatedMillis"

    /** Convierte [timer] en un mapa para Firestore. */
    fun toMap(timer: ActiveTimer): Map<String, Any?> = mapOf(
        FIELD_TASK_ID to timer.taskId,
        FIELD_STARTED_AT_MILLIS to timer.startedAt.toEpochMilliseconds(),
        FIELD_RUNNING_SINCE_MILLIS to timer.runningSince?.toEpochMilliseconds(),
        FIELD_ACCUMULATED_MILLIS to timer.accumulated.inWholeMilliseconds,
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
}
