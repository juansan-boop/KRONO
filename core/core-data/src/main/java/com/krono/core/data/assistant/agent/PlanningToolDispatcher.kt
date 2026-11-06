package com.krono.core.data.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.data.assistant.agent.PlanningToolDeclarations.Params
import com.krono.core.data.assistant.label
import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.domain.assistant.agent.PlanningAgentError
import com.krono.core.domain.assistant.agent.PlanningAgentSession
import com.krono.core.domain.assistant.agent.TaskDraft
import com.krono.core.domain.assistant.agent.TaskDraftRequest
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

/** Llamada de Gemini a una herramienta: nombre y argumentos tal como llegan en JSON. */
internal data class ToolCall(val name: String, val args: Map<String, JsonElement>)

/**
 * Traduce las llamadas de Gemini a la `PlanningAgentSession` (F-28) y sus resultados a JSON.
 *
 * Nunca lanza: un argumento inválido o una herramienta desconocida se responde con
 * `{"ok": false, "error": "…"}` para que el modelo corrija la llamada en el siguiente turno.
 */
internal class PlanningToolDispatcher {

    /** Ejecuta [call] sobre [session] y devuelve la respuesta para el modelo. */
    fun dispatch(call: ToolCall, session: PlanningAgentSession): JsonObject = when (call.name) {
        PlanningToolDeclarations.CREATE_TASK -> createTask(call.args, session)
        PlanningToolDeclarations.ESTIMATE_TASK -> estimateTask(call.args, session)
        PlanningToolDeclarations.BUILD_PLAN -> planJson(session.buildPlan(), session)
        else -> failure("La herramienta \"${call.name}\" no existe.")
    }

    private fun createTask(args: Map<String, JsonElement>, session: PlanningAgentSession): JsonObject {
        val result: KronoResult<TaskDraft> = when (val parsed = parseRequest(args)) {
            is KronoResult.Success -> session.createTask(parsed.data)
            is KronoResult.Error -> parsed
            KronoResult.Loading -> KronoResult.Loading
        }
        return when (result) {
            is KronoResult.Success -> draftJson(result.data)
            is KronoResult.Error -> failure(result.throwable.message.orEmpty())
            KronoResult.Loading -> failure(UNEXPECTED_STATE)
        }
    }

    private fun estimateTask(args: Map<String, JsonElement>, session: PlanningAgentSession): JsonObject {
        val alias = args.string(Params.TASK_ID) ?: return failure("Falta \"${Params.TASK_ID}\".")
        return when (val result = session.estimateTask(alias)) {
            is KronoResult.Success -> estimateJson(result.data)
            is KronoResult.Error -> failure(result.throwable.message.orEmpty())
            KronoResult.Loading -> failure(UNEXPECTED_STATE)
        }
    }

    private fun parseRequest(args: Map<String, JsonElement>): KronoResult<TaskDraftRequest> {
        val title = args.string(Params.TITLE) ?: return invalid("Falta \"${Params.TITLE}\".")
        val category = args.enumValue<TaskCategory>(Params.CATEGORY)
            ?: return invalid("\"${Params.CATEGORY}\" debe ser uno de ${TaskCategory.entries.joinToString()}.")
        val priority = if (Params.PRIORITY in args) {
            args.enumValue<TaskPriority>(Params.PRIORITY)
                ?: return invalid("\"${Params.PRIORITY}\" debe ser uno de ${TaskPriority.entries.joinToString()}.")
        } else {
            TaskPriority.MEDIA
        }
        val dueText = args.string(Params.DUE_DATE)
        val dueDate = dueText?.let { parseDueDate(it) }
        if (dueText != null && dueDate == null) {
            return invalid("\"${Params.DUE_DATE}\" debe tener el formato yyyy-MM-ddTHH:mm.")
        }
        val minutes = args.number(Params.ESTIMATED_MINUTES)?.roundToInt()
        return KronoResult.Success(TaskDraftRequest(title, category, priority, dueDate, minutes?.minutes))
    }

    private fun invalid(reason: String): KronoResult<Nothing> =
        KronoResult.Error(PlanningAgentError.InvalidDraft(reason))

    private fun draftJson(draft: TaskDraft): JsonObject = buildJsonObject {
        put(OK, true)
        put("id", draft.alias)
        put("titulo", draft.request.title)
        draft.request.dueDate?.let { put("fecha_limite", it.toString()) }
    }

    private fun estimateJson(draft: TaskDraft): JsonObject = buildJsonObject {
        put(OK, true)
        put("id", draft.alias)
        draft.estimate?.let {
            put("minutos", it.duration.inWholeMinutes)
            put("origen", it.source.sourceLabel())
        }
        put("nota", "Duración calculada por H1. Úsala tal cual.")
    }

    private fun planJson(plan: PriorityPlan?, session: PlanningAgentSession): JsonObject = buildJsonObject {
        put(OK, true)
        if (plan == null) {
            put("nota", "No hay tareas pendientes.")
            return@buildJsonObject
        }
        put("nota", "Orden calculado por la heurística H2 de KRONO. Cuéntalo en este orden, sin reordenarlo.")
        val newIds = session.drafts.associate { it.id to it.alias }
        putJsonArray("bloques") {
            (listOf(plan.recommended) + plan.sequence).forEachIndexed { index, planned ->
                addJsonObject { putBlock(index + 1, planned, newIds[planned.task.id]) }
            }
        }
    }

    private fun JsonObjectBuilder.putBlock(order: Int, planned: PlannedTask, alias: String?) {
        put("orden", order)
        put("titulo", planned.task.title)
        put("nueva", alias != null)
        alias?.let { put("id", it) }
        put("inicio", planned.start.toString())
        put("fin", planned.end.toString())
        put("minutos", planned.estimate.inWholeMinutes)
        putJsonArray("motivos") { planned.factors.forEach { add(it.label()) } }
    }

    private fun failure(reason: String): JsonObject = buildJsonObject {
        put(OK, false)
        put("error", reason)
    }

    private companion object {
        const val OK = "ok"
        const val UNEXPECTED_STATE = "Estado inesperado de la herramienta."

        // Si el modelo solo da el día, la tarea vence al final de ese día.
        val END_OF_DAY = LocalTime(hour = 23, minute = 59)

        fun parseDueDate(text: String): LocalDateTime? =
            runCatching { LocalDateTime.parse(text.trim()) }.getOrNull()
                ?: runCatching { LocalDate.parse(text.trim()).atTime(END_OF_DAY) }.getOrNull()

        fun Map<String, JsonElement>.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

        // Gemini a veces manda los enteros como 90.0.
        fun Map<String, JsonElement>.number(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

        inline fun <reified E : Enum<E>> Map<String, JsonElement>.enumValue(key: String): E? =
            string(key)?.let { value -> enumValues<E>().firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } }

        fun EstimateSource.sourceLabel(): String = when (this) {
            EstimateSource.MANUAL -> "estimación del usuario, ajustada con su historial si hay datos"
            EstimateSource.HISTORY -> "historial real del cronómetro"
            EstimateSource.DEFAULT -> "duración por defecto, sin historial"
        }
    }
}
