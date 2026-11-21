package com.krono.core.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.krono.core.domain.sync.SyncStatus
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds

/**
 * Entidad de persistencia local en Room para la caché estructurada de tareas (F-02, D-1).
 *
 * Almacena las tareas asociadas al identificador del usuario (`userId`) y su estado
 * de sincronización local ("CONFIRMED", "PENDING_SYNC", "FAILED").
 *
 * @property id Identificador único de la tarea (Clave primaria).
 * @property userId Identificador del usuario propietario de la tarea en Firebase Auth.
 * @property title Título visible de la tarea.
 * @property category Nombre serializado de la categoría de dominio ([TaskCategory]).
 * @property priority Nombre serializado de la prioridad de dominio ([TaskPriority]).
 * @property dueDateIso Cadena de fecha y hora límite en formato ISO-8601, o `null` si no aplica.
 * @property estimatedDurationMillis Duración estimada en milisegundos, o `null` si no se ha fijado.
 * @property actualDurationMillis Tiempo total acumulado de trabajo medido en milisegundos.
 * @property isCompleted Indica si la tarea se encuentra finalizada.
 * @property note Anotaciones o descripción adicional de la tarea, o `null` si no posee.
 * @property subtasksJson Lista de subtareas embebidas serializada en formato JSON con kotlinx.serialization.
 * @property isDeleted Indica si la tarea ha sido descartada o eliminada lógicamente.
 * @property syncStatus Estado de sincronización respecto al servidor remoto ("CONFIRMED", "PENDING_SYNC", "FAILED").
 * @property completedAtIso Cadena de fecha y hora en que se completó la tarea en formato ISO-8601, o `null` si no aplica.
 */
@Entity(tableName = "tasks_cache")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val priority: String,
    val dueDateIso: String?,
    val estimatedDurationMillis: Long?,
    val actualDurationMillis: Long,
    val isCompleted: Boolean,
    val note: String?,
    val subtasksJson: String,
    val isDeleted: Boolean,
    val syncStatus: String,
    val completedAtIso: String? = null,
) {
    /**
     * Convierte la entidad de caché de Room al modelo de dominio puro de KRONO ([Task]).
     *
     * @return Instancia desacoplada de [Task].
     */
    fun toDomain(): Task {
        val parsedDueDate = dueDateIso?.let {
            try {
                LocalDateTime.parse(it)
            } catch (_: Exception) {
                null
            }
        }

        val parsedSubtasks = if (subtasksJson.isBlank()) {
            emptyList()
        } else {
            try {
                val element = Json.parseToJsonElement(subtasksJson)
                element.jsonArray.mapNotNull { item ->
                    val obj = item.jsonObject
                    val subtaskId = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val subtaskTaskId = obj["taskId"]?.jsonPrimitive?.content ?: ""
                    val subtaskTitle = obj["title"]?.jsonPrimitive?.content ?: ""
                    val isDone = obj["isDone"]?.jsonPrimitive?.booleanOrNull ?: false
                    val position = obj["position"]?.jsonPrimitive?.intOrNull ?: 0
                    Subtask(
                        id = subtaskId,
                        taskId = subtaskTaskId,
                        title = subtaskTitle,
                        isDone = isDone,
                        position = position,
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val parsedCategory = try {
            TaskCategory.valueOf(category)
        } catch (_: Exception) {
            TaskCategory.PERSONAL
        }

        val parsedPriority = try {
            TaskPriority.valueOf(priority)
        } catch (_: Exception) {
            TaskPriority.MEDIA
        }

        val parsedSyncStatus = when (syncStatus) {
            STATUS_CONFIRMED -> SyncStatus.CONFIRMED
            STATUS_PENDING_SYNC -> SyncStatus.PENDING_SYNC
            STATUS_FAILED -> SyncStatus.FAILED
            STATUS_REJECTED -> SyncStatus.REJECTED
            else -> try {
                SyncStatus.valueOf(syncStatus)
            } catch (_: Exception) {
                SyncStatus.CONFIRMED
            }
        }

        val parsedCompletedAt = completedAtIso?.let {
            try {
                LocalDateTime.parse(it)
            } catch (_: Exception) {
                null
            }
        }

        return Task(
            id = id,
            title = title,
            category = parsedCategory,
            priority = parsedPriority,
            dueDate = parsedDueDate,
            estimatedDuration = estimatedDurationMillis?.milliseconds,
            actualDuration = actualDurationMillis.milliseconds,
            isCompleted = isCompleted,
            note = note,
            subtasks = parsedSubtasks,
            isDeleted = isDeleted,
            syncStatus = parsedSyncStatus,
            completedAt = parsedCompletedAt,
        )
    }

    companion object {
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_PENDING_SYNC = "PENDING_SYNC"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_REJECTED = "REJECTED"

        /**
         * Mapea una tarea del dominio [Task] a la entidad [TaskEntity] lista para persistencia en Room.
         *
         * @param task Modelo de dominio a convertir.
         * @param userId Identificador del usuario propietario.
         * @param syncStatus Estado de sincronización remota (por defecto [STATUS_CONFIRMED]).
         * @return Instancia mapeada de [TaskEntity].
         */
        fun fromDomain(
            task: Task,
            userId: String,
            syncStatus: String = when (task.syncStatus) {
                SyncStatus.CONFIRMED -> STATUS_CONFIRMED
                SyncStatus.PENDING_SYNC -> STATUS_PENDING_SYNC
                SyncStatus.FAILED -> STATUS_FAILED
                SyncStatus.REJECTED -> STATUS_REJECTED
            },
        ): TaskEntity {
            val subtasksSerialized = try {
                buildJsonArray {
                    task.subtasks.forEach { subtask ->
                        add(
                            buildJsonObject {
                                put("id", subtask.id)
                                put("taskId", subtask.taskId)
                                put("title", subtask.title)
                                put("isDone", subtask.isDone)
                                put("position", subtask.position)
                            }
                        )
                    }
                }.toString()
            } catch (_: Exception) {
                "[]"
            }

            return TaskEntity(
                id = task.id,
                userId = userId,
                title = task.title,
                category = task.category.name,
                priority = task.priority.name,
                dueDateIso = task.dueDate?.toString(),
                estimatedDurationMillis = task.estimatedDuration?.inWholeMilliseconds,
                actualDurationMillis = task.actualDuration.inWholeMilliseconds,
                isCompleted = task.isCompleted,
                note = task.note,
                subtasksJson = subtasksSerialized,
                isDeleted = task.isDeleted,
                syncStatus = syncStatus,
                completedAtIso = task.completedAt?.toString(),
            )
        }

        /**
         * Sobrecarga tipada para mapear [Task] a [TaskEntity] usando [SyncStatus].
         */
        fun fromDomain(
            task: Task,
            userId: String,
            syncStatus: SyncStatus,
        ): TaskEntity = fromDomain(
            task = task,
            userId = userId,
            syncStatus = when (syncStatus) {
                SyncStatus.CONFIRMED -> STATUS_CONFIRMED
                SyncStatus.PENDING_SYNC -> STATUS_PENDING_SYNC
                SyncStatus.FAILED -> STATUS_FAILED
                SyncStatus.REJECTED -> STATUS_REJECTED
            },
        )
    }
}

/**
 * Función de extensión para transformar un [Task] del dominio a [TaskEntity] especificando el estado en String.
 *
 * @param userId Identificador del usuario activo.
 * @param syncStatus Estado de sincronización local como cadena.
 * @return Instancia correspondiente de [TaskEntity].
 */
fun Task.toEntity(userId: String, syncStatus: String): TaskEntity =
    TaskEntity.fromDomain(this, userId, syncStatus)

/**
 * Función de extensión para transformar un [Task] del dominio a [TaskEntity] con [SyncStatus].
 *
 * @param userId Identificador del usuario activo.
 * @param syncStatus Estado de sincronización local tipado (por defecto toma el de la tarea).
 * @return Instancia correspondiente de [TaskEntity].
 */
fun Task.toEntity(userId: String, syncStatus: SyncStatus = this.syncStatus): TaskEntity =
    TaskEntity.fromDomain(this, userId, syncStatus)
