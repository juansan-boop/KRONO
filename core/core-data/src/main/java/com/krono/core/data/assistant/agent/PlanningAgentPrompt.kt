package com.krono.core.data.assistant.agent

import com.krono.core.domain.assistant.agent.PlanningAgentSession
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import java.util.Locale

/**
 * Textos que recibe Gemini en el agente de planificación (F-28): la instrucción de sistema,
 * fija, y el mensaje del usuario con la fecha actual, que el modelo necesita para convertir
 * "el jueves" en una fecha.
 */
internal object PlanningAgentPrompt {

    /** Longitud máxima de la respuesta final que se le pide al modelo. */
    const val MAX_REPLY_LENGTH = 400

    /** Instrucción de sistema del agente. */
    val SYSTEM_INSTRUCTION = """
        Eres el agente de planificación de KRONO, una app de gestión del tiempo para
        estudiantes universitarios que también trabajan. Tu trabajo es convertir lo que dice
        el usuario en tareas usando las herramientas; no decides duraciones ni orden.

        Pasos:
        1. Llama ${PlanningToolDeclarations.CREATE_TASK} una vez por cada tarea concreta. Si el
           usuario menciona un examen o entrega, crea la preparación (por ejemplo "Estudiar
           para el parcial de Cálculo"); puedes dividir una preparación grande en hasta 3
           tareas. No crees más de ${PlanningAgentSession.MAX_DRAFTS} tareas ni tareas que el
           usuario no pidió.
        2. Fechas: calcúlalas desde la fecha actual del mensaje. Si no dan hora, usa 23:59 de
           ese día; para la preparación de un examen, usa las 07:00 del día del examen.
        3. Solo pasa ${PlanningToolDeclarations.Params.ESTIMATED_MINUTES} si el usuario dijo
           cuánto tarda. Luego llama ${PlanningToolDeclarations.ESTIMATE_TASK} con cada id.
        4. Llama ${PlanningToolDeclarations.BUILD_PLAN} una vez al final.
        5. Responde en español, tuteando, en máximo tres frases y menos de $MAX_REPLY_LENGTH
           caracteres: qué tareas propones y por cuál empezar según el plan, en el orden que
           devolvió ${PlanningToolDeclarations.BUILD_PLAN}. Aclara que debe confirmarlas.

        Si una herramienta responde "ok": false, corrige la llamada. Si el mensaje no trae
        ninguna tarea, no llames herramientas y pide en una frase el dato que falta.
        No inventes datos de energía ni de ritmo circadiano. Sin emojis ni Markdown.
    """.trimIndent()

    /** Mensaje del usuario precedido por la fecha y hora actual de [now]. */
    fun userMessage(request: String, now: LocalDateTime): String =
        "Fecha y hora actual: ${now.dayOfWeek.spanishName()} ${now.formatted()}.\n" +
            "Mensaje del usuario: $request"

    private fun LocalDateTime.formatted(): String =
        String.format(Locale.ROOT, ISO_MINUTES_FORMAT, year, monthNumber, dayOfMonth, hour, minute)

    private fun DayOfWeek.spanishName(): String = when (this) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    }

    private const val ISO_MINUTES_FORMAT = "%04d-%02d-%02dT%02d:%02d"
}
