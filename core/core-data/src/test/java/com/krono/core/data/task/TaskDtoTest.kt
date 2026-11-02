package com.krono.core.data.task

import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias para la conversión y serialización de [TaskDto] y [SubtaskDto].
 */
class TaskDtoTest {

    @Test
    fun toDomain_convierteTodosLosCamposCorrectamente() {
        val subtask = Subtask(
            id = "s1",
            taskId = "t1",
            title = "Subtarea 1",
            isDone = true,
            position = 1,
        )
        val task = Task(
            id = "t1",
            title = "Entregar informe",
            category = TaskCategory.TRABAJO,
            priority = TaskPriority.ALTA,
            dueDate = LocalDateTime(2026, 10, 15, 14, 30),
            estimatedDuration = 45.minutes,
            actualDuration = 30.minutes,
            isCompleted = true,
            note = "Nota importante",
            subtasks = listOf(subtask),
        )

        val dto = task.toDto()
        val domain = dto.toDomain()

        assertEquals(task.id, domain.id)
        assertEquals(task.title, domain.title)
        assertEquals(task.category, domain.category)
        assertEquals(task.priority, domain.priority)
        assertEquals(task.dueDate, domain.dueDate)
        assertEquals(task.estimatedDuration, domain.estimatedDuration)
        assertEquals(task.actualDuration, domain.actualDuration)
        assertTrue(domain.isCompleted)
        assertEquals(task.note, domain.note)
        assertEquals(1, domain.subtasks.size)
        assertEquals("s1", domain.subtasks[0].id)
        assertTrue(domain.subtasks[0].isDone)
    }

    @Test
    fun toMap_y_fromMap_preservanDatosDeTarea() {
        val subtaskMap = mapOf(
            SubtaskDto.FIELD_ID to "s1",
            SubtaskDto.FIELD_TASK_ID to "t1",
            SubtaskDto.FIELD_TITLE to "Subtarea",
            SubtaskDto.FIELD_IS_DONE to false,
            SubtaskDto.FIELD_POSITION to 0,
        )
        val originalMap = mapOf(
            TaskDto.FIELD_ID to "t1",
            TaskDto.FIELD_TITLE to "Tarea prueba",
            TaskDto.FIELD_CATEGORY to "ESTUDIO",
            TaskDto.FIELD_PRIORITY to "BAJA",
            TaskDto.FIELD_DUE_DATE to "2026-10-20T10:00:00",
            TaskDto.FIELD_ESTIMATED_DURATION_MILLIS to 3600000L,
            TaskDto.FIELD_ACTUAL_DURATION_MILLIS to 1800000L,
            TaskDto.FIELD_IS_COMPLETED to false,
            TaskDto.FIELD_NOTE to "Una nota",
            TaskDto.FIELD_SUBTASKS to listOf(subtaskMap),
        )

        val dto = TaskDto.fromMap("t1", originalMap)
        assertEquals("t1", dto.id)
        assertEquals("Tarea prueba", dto.title)
        assertEquals("ESTUDIO", dto.category)
        assertEquals("BAJA", dto.priority)

        val reconstructedMap = dto.toMap()
        assertEquals("t1", reconstructedMap[TaskDto.FIELD_ID])
        assertEquals("Tarea prueba", reconstructedMap[TaskDto.FIELD_TITLE])
        assertEquals("ESTUDIO", reconstructedMap[TaskDto.FIELD_CATEGORY])
    }

    @Test
    fun toDomain_conValoresDesconocidos_aplicaFallbacksPorDefecto() {
        val dto = TaskDto(
            id = "t2",
            title = "Tarea con fallbacks",
            category = "VALOR_INVENTADO",
            priority = "OTRA_PRIORIDAD",
            dueDateIso = "fecha-invalida",
            estimatedDurationMillis = null,
        )

        val domain = dto.toDomain()
        assertEquals(TaskCategory.PERSONAL, domain.category)
        assertEquals(TaskPriority.MEDIA, domain.priority)
        assertNull(domain.dueDate)
        assertNull(domain.estimatedDuration)
        assertFalse(domain.isCompleted)
    }

    @Test
    fun subtasks_seOrdenanPorPosicion() {
        val s2 = mapOf(SubtaskDto.FIELD_ID to "s2", SubtaskDto.FIELD_POSITION to 2)
        val s1 = mapOf(SubtaskDto.FIELD_ID to "s1", SubtaskDto.FIELD_POSITION to 1)
        val dto = TaskDto(id = "t3", title = "Con subtareas", subtasks = listOf(s2, s1))

        val domain = dto.toDomain()
        assertEquals("s1", domain.subtasks[0].id)
        assertEquals("s2", domain.subtasks[1].id)
    }

    @Test
    fun toPartialMap_conCamposDelFormulario_soloIncluyeEsosCampos() {
        val dto = TaskDto(
            id = "t1",
            title = "Editada",
            category = TaskCategory.TRABAJO.name,
            priority = TaskPriority.ALTA.name,
            dueDateIso = "2026-10-15T14:30",
            estimatedDurationMillis = 45.minutes.inWholeMilliseconds,
            actualDurationMillis = 0L,
            isCompleted = false,
        )

        val partial = dto.toPartialMap(TaskField.EDIT_FORM)

        assertEquals(
            setOf(
                TaskDto.FIELD_TITLE,
                TaskDto.FIELD_CATEGORY,
                TaskDto.FIELD_PRIORITY,
                TaskDto.FIELD_DUE_DATE,
                TaskDto.FIELD_ESTIMATED_DURATION_MILLIS,
            ),
            partial.keys,
        )
        assertEquals("Editada", partial[TaskDto.FIELD_TITLE])
        assertEquals("2026-10-15T14:30", partial[TaskDto.FIELD_DUE_DATE])
    }

    @Test
    fun toPartialMap_cubreCadaCampoParcialSinIncluirElTiempoReal() {
        val partial = TaskDto(id = "t1").toPartialMap(TaskField.entries.toSet())

        assertEquals(TaskField.entries.size, partial.size)
        assertFalse(partial.containsKey(TaskDto.FIELD_ACTUAL_DURATION_MILLIS))
        assertFalse(partial.containsKey(TaskDto.FIELD_ID))
    }
}
