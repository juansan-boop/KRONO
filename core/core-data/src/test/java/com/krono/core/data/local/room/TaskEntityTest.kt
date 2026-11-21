package com.krono.core.data.local.room

import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas unitarias exhaustivas para [TaskEntity] y sus mappers bidireccionales con el modelo
 * de dominio [Task] (F-02, D-1).
 *
 * Valida la fidelidad de todos los campos, estados de sincronización remota ([TaskEntity.STATUS_CONFIRMED],
 * [TaskEntity.STATUS_PENDING_SYNC], [TaskEntity.STATUS_FAILED]), serialización/deserialización
 * JSON de subtareas embebidas, resiliencia ante datos corruptos y preservación en viajes de ida y vuelta.
 */
class TaskEntityTest {

    private val sampleDueDate = LocalDateTime(2026, 11, 20, 15, 30, 0)

    private val sampleSubtasks = listOf(
        Subtask(id = "sub-1", taskId = "task-100", title = "Paso inicial", isDone = true, position = 0),
        Subtask(id = "sub-2", taskId = "task-100", title = "Paso secundario", isDone = false, position = 1),
    )

    private val sampleTask = Task(
        id = "task-100",
        title = "Diseñar base de datos Room",
        category = TaskCategory.ESTUDIO,
        priority = TaskPriority.ALTA,
        dueDate = sampleDueDate,
        estimatedDuration = 2.hours,
        actualDuration = 45.minutes,
        isCompleted = false,
        note = "Revisar índices e integridad referencial",
        subtasks = sampleSubtasks,
        isDeleted = false,
    )

    // =========================================================================
    // Mapeo Dominio -> Entidad (fromDomain / toEntity)
    // =========================================================================

    /**
     * Verifica que [TaskEntity.fromDomain] mapee todos los campos primitivos y complejos
     * preservando la fidelidad total hacia la entidad de Room.
     */
    @Test
    fun fromDomain_mapeaTodosLosCamposConFidelidadExhaustiva() {
        val entity = TaskEntity.fromDomain(
            task = sampleTask,
            userId = "user-abc-123",
            syncStatus = TaskEntity.STATUS_CONFIRMED,
        )

        assertEquals("task-100", entity.id)
        assertEquals("user-abc-123", entity.userId)
        assertEquals("Diseñar base de datos Room", entity.title)
        assertEquals(TaskCategory.ESTUDIO.name, entity.category)
        assertEquals(TaskPriority.ALTA.name, entity.priority)
        assertEquals("2026-11-20T15:30", entity.dueDateIso)
        assertEquals(2.hours.inWholeMilliseconds, entity.estimatedDurationMillis)
        assertEquals(45.minutes.inWholeMilliseconds, entity.actualDurationMillis)
        assertFalse(entity.isCompleted)
        assertEquals("Revisar índices e integridad referencial", entity.note)
        assertFalse(entity.isDeleted)
        assertEquals(TaskEntity.STATUS_CONFIRMED, entity.syncStatus)

        // Verificamos serialización de subtareas embebidas
        assertTrue(entity.subtasksJson.contains("sub-1"))
        assertTrue(entity.subtasksJson.contains("Paso inicial"))
        assertTrue(entity.subtasksJson.contains("sub-2"))
        assertTrue(entity.subtasksJson.contains("Paso secundario"))
    }

    /**
     * Verifica que el estado de sincronización CONFIRMED se preserve correctamente.
     */
    @Test
    fun fromDomain_conEstadoConfirmed_asignaStatusConfirmed() {
        val entity = TaskEntity.fromDomain(sampleTask, userId = "user-1", syncStatus = TaskEntity.STATUS_CONFIRMED)
        assertEquals(TaskEntity.STATUS_CONFIRMED, entity.syncStatus)
    }

    /**
     * Verifica que el estado de sincronización PENDING_SYNC se preserve correctamente al crear o editar offline.
     */
    @Test
    fun fromDomain_conEstadoPendingSync_asignaStatusPendingSync() {
        val entity = TaskEntity.fromDomain(sampleTask, userId = "user-1", syncStatus = TaskEntity.STATUS_PENDING_SYNC)
        assertEquals(TaskEntity.STATUS_PENDING_SYNC, entity.syncStatus)
    }

    /**
     * Verifica que el estado de sincronización FAILED se preserve correctamente ante rechazos del servidor.
     */
    @Test
    fun fromDomain_conEstadoFailed_asignaStatusFailed() {
        val entity = TaskEntity.fromDomain(sampleTask, userId = "user-1", syncStatus = TaskEntity.STATUS_FAILED)
        assertEquals(TaskEntity.STATUS_FAILED, entity.syncStatus)
    }

    /**
     * Verifica que el estado de sincronización REJECTED se preserve correctamente ante rechazos por reglas del servidor.
     */
    @Test
    fun fromDomain_conEstadoRejected_asignaStatusRejected() {
        val entity = TaskEntity.fromDomain(sampleTask, userId = "user-1", syncStatus = TaskEntity.STATUS_REJECTED)
        assertEquals(TaskEntity.STATUS_REJECTED, entity.syncStatus)
    }

    /**
     * Verifica que toDomain mapee fielmente todos los estados de sincronización.
     */
    @Test
    fun toDomain_mapeaTodosLosEstadosDeSincronizacion() {
        val entityConfirmed = sampleTask.toEntity("u-1", TaskEntity.STATUS_CONFIRMED)
        assertEquals(com.krono.core.domain.sync.SyncStatus.CONFIRMED, entityConfirmed.toDomain().syncStatus)

        val entityPending = sampleTask.toEntity("u-1", TaskEntity.STATUS_PENDING_SYNC)
        assertEquals(com.krono.core.domain.sync.SyncStatus.PENDING_SYNC, entityPending.toDomain().syncStatus)

        val entityFailed = sampleTask.toEntity("u-1", TaskEntity.STATUS_FAILED)
        assertEquals(com.krono.core.domain.sync.SyncStatus.FAILED, entityFailed.toDomain().syncStatus)

        val entityRejected = sampleTask.toEntity("u-1", TaskEntity.STATUS_REJECTED)
        assertEquals(com.krono.core.domain.sync.SyncStatus.REJECTED, entityRejected.toDomain().syncStatus)
    }

    /**
     * Verifica que si no se provee syncStatus en fromDomain, el valor por defecto sea STATUS_CONFIRMED.
     */
    @Test
    fun fromDomain_sinEstadoExplicito_usaConfirmedPorDefecto() {
        val entity = TaskEntity.fromDomain(sampleTask, userId = "user-default")
        assertEquals(TaskEntity.STATUS_CONFIRMED, entity.syncStatus)
    }

    /**
     * Verifica que la función de extensión [toEntity] asigne correctamente los valores por defecto y personalizados.
     */
    @Test
    fun toEntity_extension_asignaUserIdYStatusCorrectamente() {
        val entityDefault = sampleTask.toEntity("user-42")
        assertEquals("user-42", entityDefault.userId)
        assertEquals(TaskEntity.STATUS_CONFIRMED, entityDefault.syncStatus)

        val entityPending = sampleTask.toEntity("user-42", TaskEntity.STATUS_PENDING_SYNC)
        assertEquals("user-42", entityPending.userId)
        assertEquals(TaskEntity.STATUS_PENDING_SYNC, entityPending.syncStatus)

        val entityFailed = sampleTask.toEntity("user-42", TaskEntity.STATUS_FAILED)
        assertEquals("user-42", entityFailed.userId)
        assertEquals(TaskEntity.STATUS_FAILED, entityFailed.syncStatus)
    }

    /**
     * Verifica el mapeo de campos opcionales nulos (dueDate = null, estimatedDuration = null, note = null)
     * y listas vacías de subtareas.
     */
    @Test
    fun fromDomain_conCamposOpcionalesNulos_generaValoresNulosYJsonVacio() {
        val minimalTask = Task(
            id = "t-min",
            title = "Tarea Mínima",
            dueDate = null,
            estimatedDuration = null,
            note = null,
            subtasks = emptyList(),
        )

        val entity = minimalTask.toEntity("user-min")

        assertNull(entity.dueDateIso)
        assertNull(entity.estimatedDurationMillis)
        assertNull(entity.note)
        assertEquals("[]", entity.subtasksJson)
        assertEquals(0L, entity.actualDurationMillis)
        assertFalse(entity.isCompleted)
        assertFalse(entity.isDeleted)
    }

    // =========================================================================
    // Mapeo Entidad -> Dominio (toDomain)
    // =========================================================================

    /**
     * Verifica que [TaskEntity.toDomain] reconstruya fielmente todos los campos del modelo de dominio.
     */
    @Test
    fun toDomain_reconstruyeFielmenteElModeloDeDominio() {
        val entity = TaskEntity(
            id = "t-2",
            userId = "user-abc",
            title = "Reporte Trimestral",
            category = "TRABAJO",
            priority = "MEDIA",
            dueDateIso = "2026-12-01T09:00",
            estimatedDurationMillis = 3600000L,
            actualDurationMillis = 1800000L,
            isCompleted = true,
            note = "Entregar al director",
            subtasksJson = """[{"id":"s-10","taskId":"t-2","title":"Recopilar métricas","isDone":true,"position":0}]""",
            isDeleted = false,
            syncStatus = TaskEntity.STATUS_CONFIRMED,
        )

        val domain = entity.toDomain()

        assertEquals("t-2", domain.id)
        assertEquals("Reporte Trimestral", domain.title)
        assertEquals(TaskCategory.TRABAJO, domain.category)
        assertEquals(TaskPriority.MEDIA, domain.priority)
        assertEquals(LocalDateTime(2026, 12, 1, 9, 0, 0), domain.dueDate)
        assertEquals(1.hours, domain.estimatedDuration)
        assertEquals(30.minutes, domain.actualDuration)
        assertTrue(domain.isCompleted)
        assertEquals("Entregar al director", domain.note)
        assertEquals(1, domain.subtasks.size)
        assertEquals("s-10", domain.subtasks[0].id)
        assertEquals("t-2", domain.subtasks[0].taskId)
        assertEquals("Recopilar métricas", domain.subtasks[0].title)
        assertTrue(domain.subtasks[0].isDone)
        assertEquals(0, domain.subtasks[0].position)
        assertFalse(domain.isDeleted)
    }

    /**
     * Verifica la deserialización de múltiples subtareas embebidas preservando orden, identificadores y booleanos.
     */
    @Test
    fun toDomain_conSubtareasMultiples_preservaOrdenYCamposCompletos() {
        val subtasksJson = """
            [
                {"id":"s-1","taskId":"t-multi","title":"Paso 1","isDone":true,"position":0},
                {"id":"s-2","taskId":"t-multi","title":"Paso 2","isDone":false,"position":1},
                {"id":"s-3","taskId":"t-multi","title":"Paso 3","isDone":true,"position":2}
            ]
        """.trimIndent()

        val entity = TaskEntity(
            id = "t-multi",
            userId = "u-1",
            title = "Tarea con subtareas",
            category = "PERSONAL",
            priority = "BAJA",
            dueDateIso = null,
            estimatedDurationMillis = null,
            actualDurationMillis = 0L,
            isCompleted = false,
            note = null,
            subtasksJson = subtasksJson,
            isDeleted = false,
            syncStatus = TaskEntity.STATUS_CONFIRMED,
        )

        val domain = entity.toDomain()

        assertEquals(3, domain.subtasks.size)
        assertEquals("s-1", domain.subtasks[0].id)
        assertTrue(domain.subtasks[0].isDone)
        assertEquals(0, domain.subtasks[0].position)

        assertEquals("s-2", domain.subtasks[1].id)
        assertFalse(domain.subtasks[1].isDone)
        assertEquals(1, domain.subtasks[1].position)

        assertEquals("s-3", domain.subtasks[2].id)
        assertTrue(domain.subtasks[2].isDone)
        assertEquals(2, domain.subtasks[2].position)
    }

    /**
     * Verifica que si subtasksJson está vacío o compuesto de espacios en blanco, retorne una lista vacía.
     */
    @Test
    fun toDomain_conSubtasksJsonVacioOEnBlanco_retornaListaVacia() {
        val entityBlank = sampleTask.toEntity("user-1").copy(subtasksJson = "   ")
        assertTrue(entityBlank.toDomain().subtasks.isEmpty())

        val entityEmpty = sampleTask.toEntity("user-1").copy(subtasksJson = "")
        assertTrue(entityEmpty.toDomain().subtasks.isEmpty())
    }

    /**
     * Verifica que si subtasksJson contiene un JSON mal formado o corrupto, no falle y retorne lista vacía.
     */
    @Test
    fun toDomain_conSubtasksJsonCorrupto_retornaListaVaciaSinLanzarExcepcion() {
        val entityCorrupt = sampleTask.toEntity("user-1").copy(subtasksJson = "{ json invalido no es un array")
        assertTrue(entityCorrupt.toDomain().subtasks.isEmpty())

        val entityMalformedScalar = sampleTask.toEntity("user-1").copy(subtasksJson = "12345")
        assertTrue(entityMalformedScalar.toDomain().subtasks.isEmpty())
    }

    /**
     * Verifica que si un elemento de subtarea en el JSON carece de campos opcionales, aplique defaults seguros.
     */
    @Test
    fun toDomain_conSubtareasConCamposFaltantes_aplicaValoresPorDefectoSeguros() {
        val jsonIncomplete = """[{"id":"sub-only-id"}]"""
        val entity = sampleTask.toEntity("user-1").copy(subtasksJson = jsonIncomplete)

        val domain = entity.toDomain()
        assertEquals(1, domain.subtasks.size)
        assertEquals("sub-only-id", domain.subtasks[0].id)
        assertEquals("", domain.subtasks[0].taskId)
        assertEquals("", domain.subtasks[0].title)
        assertFalse(domain.subtasks[0].isDone)
        assertEquals(0, domain.subtasks[0].position)
    }

    /**
     * Verifica que todas las categorías de dominio válidas se mapeen fielmente de ida y vuelta.
     */
    @Test
    fun toDomain_conTodasLasCategoriasValidas_mapeaEnumCorrespondiente() {
        for (category in TaskCategory.entries) {
            val task = sampleTask.copy(category = category)
            val entity = task.toEntity("u-1")
            assertEquals(category.name, entity.category)
            assertEquals(category, entity.toDomain().category)
        }
    }

    /**
     * Verifica que todas las prioridades de dominio válidas se mapeen fielmente de ida y vuelta.
     */
    @Test
    fun toDomain_conTodasLasPrioridadesValidas_mapeaEnumCorrespondiente() {
        for (priority in TaskPriority.entries) {
            val task = sampleTask.copy(priority = priority)
            val entity = task.toEntity("u-1")
            assertEquals(priority.name, entity.priority)
            assertEquals(priority, entity.toDomain().priority)
        }
    }

    /**
     * Verifica que ante una categoría desconocida o inventada en Room, se use el valor por defecto [TaskCategory.PERSONAL].
     */
    @Test
    fun toDomain_conCategoriaDesconocida_usaCategoriaPersonalPorDefecto() {
        val entity = sampleTask.toEntity("user-1").copy(category = "CATEGORIA_INVENTADA")
        assertEquals(TaskCategory.PERSONAL, entity.toDomain().category)
    }

    /**
     * Verifica que ante una prioridad desconocida o corrupta en Room, se use el valor por defecto [TaskPriority.MEDIA].
     */
    @Test
    fun toDomain_conPrioridadDesconocida_usaPrioridadMediaPorDefecto() {
        val entity = sampleTask.toEntity("user-1").copy(priority = "PRIORIDAD_DESCONOCIDA")
        assertEquals(TaskPriority.MEDIA, entity.toDomain().priority)
    }

    /**
     * Verifica que ante una fecha ISO mal formada o no parseable, retorne dueDate = null sin lanzar excepción.
     */
    @Test
    fun toDomain_conFechaIsoInvalida_retornaDueDateNull() {
        val entity = sampleTask.toEntity("user-1").copy(dueDateIso = "cadena-fecha-invalida")
        assertNull(entity.toDomain().dueDate)
    }

    /**
     * Verifica que una fecha ISO válida se parsea correctamente a [LocalDateTime].
     */
    @Test
    fun toDomain_conFechaIsoValida_parseaLocalDateTimeCorrectamente() {
        val entity = sampleTask.toEntity("user-1").copy(dueDateIso = "2026-12-31T23:59:59")
        assertEquals(LocalDateTime(2026, 12, 31, 23, 59, 59), entity.toDomain().dueDate)
    }

    /**
     * Verifica que el flag isDeleted se mapee fielmente en verdadero y falso.
     */
    @Test
    fun toDomain_y_fromDomain_conIsDeleted_preservaEstadoLogico() {
        val taskDeleted = sampleTask.copy(isDeleted = true)
        val entityDeleted = taskDeleted.toEntity("user-1")
        assertTrue(entityDeleted.isDeleted)
        assertTrue(entityDeleted.toDomain().isDeleted)

        val taskActive = sampleTask.copy(isDeleted = false)
        val entityActive = taskActive.toEntity("user-1")
        assertFalse(entityActive.isDeleted)
        assertFalse(entityActive.toDomain().isDeleted)
    }

    // =========================================================================
    // Pruebas de Ida y Vuelta (Roundtrip)
    // =========================================================================

    /**
     * Verifica que un viaje de ida y vuelta (Task -> TaskEntity -> Task) conserve el 100%
     * de los datos de una tarea compleja sin alteraciones ni pérdidas de precisión.
     */
    @Test
    fun roundtrip_tareaCompleta_conservaTodosLosDatosSinPerdidas() {
        val entity = sampleTask.toEntity(userId = "user-roundtrip", syncStatus = TaskEntity.STATUS_CONFIRMED)
        val roundtripTask = entity.toDomain()

        assertEquals(sampleTask.id, roundtripTask.id)
        assertEquals(sampleTask.title, roundtripTask.title)
        assertEquals(sampleTask.category, roundtripTask.category)
        assertEquals(sampleTask.priority, roundtripTask.priority)
        assertEquals(sampleTask.dueDate, roundtripTask.dueDate)
        assertEquals(sampleTask.estimatedDuration, roundtripTask.estimatedDuration)
        assertEquals(sampleTask.actualDuration, roundtripTask.actualDuration)
        assertEquals(sampleTask.isCompleted, roundtripTask.isCompleted)
        assertEquals(sampleTask.note, roundtripTask.note)
        assertEquals(sampleTask.subtasks, roundtripTask.subtasks)
        assertEquals(sampleTask.isDeleted, roundtripTask.isDeleted)
        assertEquals(sampleTask, roundtripTask)
    }

    /**
     * Verifica que un viaje de ida y vuelta para una tarea con campos opcionales nulos
     * preserve los valores nulos y los defaults sin generar excepciones.
     */
    @Test
    fun roundtrip_tareaConCamposOpcionalesNulos_conservaNulosYDefaults() {
        val minimalTask = Task(
            id = "t-minimal-roundtrip",
            title = "Tarea Mínima Roundtrip",
            category = TaskCategory.PERSONAL,
            priority = TaskPriority.BAJA,
            dueDate = null,
            estimatedDuration = null,
            actualDuration = 0.milliseconds,
            isCompleted = false,
            note = null,
            subtasks = emptyList(),
            isDeleted = false,
        )

        val entity = minimalTask.toEntity("user-min-rt")
        val resultTask = entity.toDomain()

        assertEquals(minimalTask, resultTask)
        assertNull(resultTask.dueDate)
        assertNull(resultTask.estimatedDuration)
        assertNull(resultTask.note)
        assertTrue(resultTask.subtasks.isEmpty())
    }
}
