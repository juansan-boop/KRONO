package com.krono.core.data.assistant.agent

import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.agent.PlanningAgentSession
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de la traducción entre las llamadas de Gemini y la sesión del agente (F-28). */
class PlanningToolDispatcherTest {

    private val dispatcher = PlanningToolDispatcher()
    private val session = PlanningAgentSession(
        currentTasks = emptyList(),
        now = LocalDateTime(2026, 11, 6, 14, 0),
        buildPriorityPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase()),
        newTaskId = { "uuid-${System.nanoTime()}" },
    )

    @Test
    fun createTask_conArgumentosValidos_creaElBorradorYDevuelveSuAlias() {
        val response = createTask(
            "title" to JsonPrimitive("Estudiar para el parcial"),
            "category" to JsonPrimitive("estudio"),
            "priority" to JsonPrimitive("ALTA"),
            "due_date" to JsonPrimitive("2026-11-12T07:00"),
            // Gemini a veces manda los enteros como decimales.
            "estimated_minutes" to JsonPrimitive(90.0),
        )

        assertTrue(response.ok())
        assertEquals("t1", response.string("id"))
        val request = session.drafts.single().request
        assertEquals(TaskCategory.ESTUDIO, request.category)
        assertEquals(TaskPriority.ALTA, request.priority)
        assertEquals(LocalDateTime(2026, 11, 12, 7, 0), request.dueDate)
        assertEquals(90.minutes, request.userEstimate)
    }

    @Test
    fun createTask_conSoloLaFecha_venceAlFinalDelDiaYPrioridadMedia() {
        createTask(
            "title" to JsonPrimitive("Entregar informe"),
            "category" to JsonPrimitive("TRABAJO"),
            "due_date" to JsonPrimitive("2026-11-13"),
        )

        val request = session.drafts.single().request
        assertEquals(LocalDateTime(2026, 11, 13, 23, 59), request.dueDate)
        assertEquals(TaskPriority.MEDIA, request.priority)
    }

    @Test
    fun createTask_conCategoriaDesconocida_respondeErrorSinCrear() {
        val response = createTask("title" to JsonPrimitive("Algo"), "category" to JsonPrimitive("OCIO"))

        assertFalse(response.ok())
        assertTrue(session.drafts.isEmpty())
    }

    @Test
    fun createTask_conFechaMalFormada_respondeError() {
        val response = createTask(
            "title" to JsonPrimitive("Algo"),
            "category" to JsonPrimitive("PERSONAL"),
            "due_date" to JsonPrimitive("el jueves"),
        )

        assertFalse(response.ok())
    }

    @Test
    fun estimateTask_devuelveLaDuracionDeH1() {
        createTask("title" to JsonPrimitive("Leer"), "category" to JsonPrimitive("ESTUDIO"))

        val response = dispatcher.dispatch(
            ToolCall(PlanningToolDeclarations.ESTIMATE_TASK, mapOf("task_id" to JsonPrimitive("t1"))),
            session,
        )

        assertTrue(response.ok())
        assertEquals(BuildPriorityPlanUseCase.DEFAULT_ESTIMATE_MINUTES.toString(), response.string("minutos"))
    }

    @Test
    fun buildPlan_marcaLasTareasNuevasEnElOrdenDeH2() {
        createTask("title" to JsonPrimitive("Leer"), "category" to JsonPrimitive("ESTUDIO"))

        val response = dispatcher.dispatch(ToolCall(PlanningToolDeclarations.BUILD_PLAN, emptyMap()), session)

        val first = response.getValue("bloques").jsonArray.first().jsonObject
        assertEquals("1", first.string("orden"))
        assertEquals("t1", first.string("id"))
        assertTrue(first.getValue("nueva").jsonPrimitive.boolean)
    }

    @Test
    fun unaHerramientaDesconocida_respondeErrorSinLanzar() {
        val response = dispatcher.dispatch(ToolCall("reorder_plan", emptyMap()), session)

        assertFalse(response.ok())
    }

    private fun createTask(vararg args: Pair<String, JsonElement>): JsonObject =
        dispatcher.dispatch(ToolCall(PlanningToolDeclarations.CREATE_TASK, args.toMap()), session)

    private fun JsonObject.ok(): Boolean = getValue("ok").jsonPrimitive.boolean

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
}
