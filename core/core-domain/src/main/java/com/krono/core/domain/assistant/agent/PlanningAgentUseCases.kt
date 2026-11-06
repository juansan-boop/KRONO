package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.UpsertTaskUseCase
import kotlinx.datetime.LocalDateTime
import java.util.UUID

/**
 * Atiende un mensaje libre del usuario con el agente de planificación (F-28): abre una
 * [PlanningAgentSession], deja que el modelo llame sus herramientas y devuelve la propuesta
 * para que el usuario la confirme. No guarda nada.
 *
 * @param newTaskId genera el identificador de cada tarea propuesta; inyectable en pruebas.
 */
class AskPlanningAgentUseCase(
    private val agent: PlanningAgent,
    private val buildPriorityPlan: BuildPriorityPlanUseCase,
    private val newTaskId: () -> String = { UUID.randomUUID().toString() },
) {

    /**
     * @param request mensaje del usuario, por ejemplo "tengo parcial el jueves, organízame la semana".
     * @param currentTasks todas las tareas del usuario.
     * @param now momento desde el que se planifica.
     * @return la propuesta, o el error que impidió armarla.
     */
    suspend operator fun invoke(
        request: String,
        currentTasks: List<Task>,
        now: LocalDateTime,
    ): KronoResult<AgentProposal> {
        val trimmed = request.trim()
        invalidReason(trimmed)?.let { return KronoResult.Error(PlanningAgentError.InvalidRequest(it)) }

        val session = PlanningAgentSession(currentTasks, now, buildPriorityPlan, newTaskId)
        return when (val result = agent.run(trimmed, session)) {
            is KronoResult.Success -> proposalOrError(session.toProposal(result.data))
            is KronoResult.Error -> result
            KronoResult.Loading -> KronoResult.Error(AssistantError.InvalidResponse)
        }
    }

    private fun invalidReason(request: String): String? = when {
        request.isEmpty() -> "Escribe qué necesitas organizar."
        request.length > MAX_REQUEST_LENGTH -> "El mensaje supera los $MAX_REQUEST_LENGTH caracteres."
        else -> null
    }

    private fun proposalOrError(proposal: AgentProposal): KronoResult<AgentProposal> =
        if (proposal.drafts.isEmpty() && proposal.message == null) {
            KronoResult.Error(PlanningAgentError.NothingProposed)
        } else {
            KronoResult.Success(proposal)
        }

    /** Límites del mensaje del usuario. */
    companion object {
        /** Longitud máxima del mensaje que se envía al modelo. */
        const val MAX_REQUEST_LENGTH = 500
    }
}

/**
 * Guarda las tareas de una [AgentProposal] que el usuario confirmó (F-28).
 *
 * Cada tarea conserva el identificador de su borrador, así que si una escritura falla y el
 * usuario reintenta, las que ya se guardaron se sobrescriben en vez de duplicarse.
 */
class ConfirmAgentProposalUseCase(
    private val upsertTask: UpsertTaskUseCase,
) {

    /** @return la cantidad de tareas creadas, o el primer error de escritura. */
    suspend operator fun invoke(proposal: AgentProposal): KronoResult<Int> {
        if (proposal.drafts.isEmpty()) {
            return KronoResult.Error(PlanningAgentError.InvalidRequest("No hay tareas para crear."))
        }
        proposal.drafts.forEach { draft ->
            val result = upsertTask(draft.toTask())
            if (result is KronoResult.Error) return result
        }
        return KronoResult.Success(proposal.drafts.size)
    }
}
