package com.krono.core.data.assistant.agent

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority

/**
 * Herramientas que Gemini puede llamar en el agente de planificación (F-28). Cada una se
 * atiende en [PlanningToolDispatcher] con la `PlanningAgentSession` de la petición.
 *
 * Los parámetros solo describen lo que dijo el usuario: no hay parámetros de orden ni de
 * duración calculada, así que el modelo no tiene cómo decidir el plan (D-5).
 */
internal object PlanningToolDeclarations {

    /** Nombre de la herramienta que propone una tarea. */
    const val CREATE_TASK = "create_task"

    /** Nombre de la herramienta que estima una tarea propuesta con H1. */
    const val ESTIMATE_TASK = "estimate_task"

    /** Nombre de la herramienta que arma el plan con H2. */
    const val BUILD_PLAN = "build_plan"

    /** Parámetros de las herramientas, compartidos con el despachador. */
    object Params {
        const val TITLE = "title"
        const val CATEGORY = "category"
        const val PRIORITY = "priority"
        const val DUE_DATE = "due_date"
        const val ESTIMATED_MINUTES = "estimated_minutes"
        const val TASK_ID = "task_id"
        const val REASON = "reason"
    }

    private val createTask = FunctionDeclaration(
        name = CREATE_TASK,
        description = "Propone crear una tarea concreta que el usuario mencionó o que hace falta " +
            "para prepararse (por ejemplo, estudiar para un parcial). No la guarda: el usuario la " +
            "confirma después. Devuelve el id corto de la tarea propuesta.",
        parameters = mapOf(
            Params.TITLE to Schema.string("Título breve en español, en infinitivo. Ej: \"Estudiar para el parcial de Cálculo\"."),
            Params.CATEGORY to Schema.enumeration(TaskCategory.entries.map { it.name }, "Categoría de la tarea."),
            Params.PRIORITY to Schema.enumeration(
                TaskPriority.entries.map { it.name },
                "Prioridad. ALTA solo si el usuario la presenta como importante o es un examen o entrega calificada.",
            ),
            Params.DUE_DATE to Schema.string(
                "Fecha y hora límite local en formato yyyy-MM-ddTHH:mm, calculada desde la fecha actual del mensaje.",
            ),
            Params.ESTIMATED_MINUTES to Schema.integer(
                "Minutos que el usuario dijo explícitamente que tarda. Omítelo si no lo dijo: KRONO lo estima.",
            ),
        ),
        optionalParameters = listOf(Params.PRIORITY, Params.DUE_DATE, Params.ESTIMATED_MINUTES),
    )

    private val estimateTask = FunctionDeclaration(
        name = ESTIMATE_TASK,
        description = "Estima la duración de una tarea propuesta con el modelo H1 de KRONO, que " +
            "aprende del tiempo real medido con el cronómetro. Usa su resultado tal cual.",
        parameters = mapOf(Params.TASK_ID to Schema.string("Id corto devuelto por create_task, por ejemplo \"t1\".")),
    )

    // Gemini no acepta bien objetos sin propiedades, así que build_plan lleva un parámetro
    // opcional que solo sirve de registro y no cambia el plan.
    private val buildPlan = FunctionDeclaration(
        name = BUILD_PLAN,
        description = "Arma el plan con la heurística H2 de KRONO usando las tareas actuales del " +
            "usuario más las propuestas. El orden lo decide H2; no lo cambies al contarlo.",
        parameters = mapOf(Params.REASON to Schema.string("Motivo breve por el que pides el plan.")),
        optionalParameters = listOf(Params.REASON),
    )

    /** Todas las herramientas del agente. */
    val all: List<FunctionDeclaration> = listOf(createTask, estimateTask, buildPlan)
}
