package com.krono.core.data.task

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * DTO para la serialización del documento de una tarea en Cloud Firestore (D-3, F-02).
 * Almacena las subtareas embebidas en el campo [subtasks] y traduce tipos complejos a primitivos.
 */
data class TaskDto(
    val id: String = "",
    val title: String = "",
    val category: String = TaskCategory.PERSONAL.name,
    val priority: String = TaskPriority.MEDIA.name,
    val dueDateIso: String? = null,
    val estimatedDurationMillis: Long? = null,
    val actualDurationMillis: Long = 0L,
    val isCompleted: Boolean = false,
    val note: String? = null,
    val subtasks: List<Map<String, Any?>> = emptyList(),
) {
    /** Convierte el DTO al modelo de dominio [Task]. */
    fun toDomain(): Task = Task(
        id = id,
        title = title,
        category = runCatching { TaskCategory.valueOf(category) }.getOrDefault(TaskCategory.PERSONAL),
        priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIA),
        dueDate = dueDateIso?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
        estimatedDuration = estimatedDurationMillis?.milliseconds,
        actualDuration = actualDurationMillis.milliseconds,
        isCompleted = isCompleted,
        note = note,
        subtasks = subtasks.map { SubtaskDto.fromMap(it).toDomain() }
            .sortedBy { it.position },
    )

    /** Serializa el DTO a un mapa compatible con Cloud Firestore. */
    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_TITLE to title,
        FIELD_CATEGORY to category,
        FIELD_PRIORITY to priority,
        FIELD_DUE_DATE to dueDateIso,
        FIELD_ESTIMATED_DURATION_MILLIS to estimatedDurationMillis,
        FIELD_ACTUAL_DURATION_MILLIS to actualDurationMillis,
        FIELD_IS_COMPLETED to isCompleted,
        FIELD_NOTE to note,
        FIELD_SUBTASKS to subtasks,
    )

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_TITLE = "title"
        const val FIELD_CATEGORY = "category"
        const val FIELD_PRIORITY = "priority"
        const val FIELD_DUE_DATE = "dueDate"
        const val FIELD_ESTIMATED_DURATION_MILLIS = "estimatedDurationMillis"
        const val FIELD_ACTUAL_DURATION_MILLIS = "actualDurationMillis"
        const val FIELD_IS_COMPLETED = "isCompleted"
        const val FIELD_NOTE = "note"
        const val FIELD_SUBTASKS = "subtasks"

        /** Crea un [TaskDto] a partir del modelo de dominio [Task]. */
        fun fromDomain(task: Task): TaskDto = TaskDto(
            id = task.id,
            title = task.title,
            category = task.category.name,
            priority = task.priority.name,
            dueDateIso = task.dueDate?.toString(),
            estimatedDurationMillis = task.estimatedDuration?.inWholeMilliseconds,
            actualDurationMillis = task.actualDuration.inWholeMilliseconds,
            isCompleted = task.isCompleted,
            note = task.note,
            subtasks = task.subtasks.map { SubtaskDto.fromDomain(it).toMap() },
        )

        /** Reconstruye un [TaskDto] a partir de los datos de un documento de Firestore. */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): TaskDto = TaskDto(
            id = (map[FIELD_ID] as? String)?.takeIf { it.isNotBlank() } ?: id,
            title = map[FIELD_TITLE] as? String ?: "",
            category = map[FIELD_CATEGORY] as? String ?: TaskCategory.PERSONAL.name,
            priority = map[FIELD_PRIORITY] as? String ?: TaskPriority.MEDIA.name,
            dueDateIso = map[FIELD_DUE_DATE] as? String,
            estimatedDurationMillis = (map[FIELD_ESTIMATED_DURATION_MILLIS] as? Number)?.toLong(),
            actualDurationMillis = (map[FIELD_ACTUAL_DURATION_MILLIS] as? Number)?.toLong() ?: 0L,
            isCompleted = map[FIELD_IS_COMPLETED] as? Boolean ?: false,
            note = map[FIELD_NOTE] as? String,
            subtasks = (map[FIELD_SUBTASKS] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty(),
        )
    }
}

/** Función de extensión para convertir cómodamente una [Task] a su DTO. */
fun Task.toDto(): TaskDto = TaskDto.fromDomain(this)
