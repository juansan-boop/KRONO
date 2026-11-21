package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.sync.SyncStatus
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
 * Resultado estructurado del guardado por lotes de tareas propuestas por el agente (F-28).
 *
 * @property confirmedTasks Tareas cuya persistencia fue confirmada remotamente por el servidor.
 * @property pendingSyncTasks Tareas persistidas en la caché local pendientes de sincronización remota.
 * @property failedTasks Detalle de tareas cuya persistencia falló, con su motivo.
 */
data class BatchSaveResult(
    val confirmedTasks: List<Task> = emptyList(),
    val pendingSyncTasks: List<Task> = emptyList(),
    val failedTasks: List<FailedTaskEntry> = emptyList(),
) {
    /** Total de tareas procesadas en el lote. */
    val totalCount: Int get() = confirmedTasks.size + pendingSyncTasks.size + failedTasks.size

    /** Cantidad total de tareas guardadas con éxito (confirmadas o encoladas localmente). */
    val successfulCount: Int get() = confirmedTasks.size + pendingSyncTasks.size

    /** Indica si todas las tareas del lote se guardaron sin errores. */
    val isAllSuccessful: Boolean get() = failedTasks.isEmpty()

    /** Indica si todas las tareas del lote fallaron. */
    val isAllFailed: Boolean get() = successfulCount == 0 && failedTasks.isNotEmpty()

    /** Indica si hubo guardado parcial con tareas exitosas y al menos una fallida. */
    val isPartial: Boolean get() = successfulCount > 0 && failedTasks.isNotEmpty()
}

/**
 * Registro de una tarea propuesta que falló al guardarse.
 *
 * @property task Tarea con su identificador original para permitir reintento idempotente.
 * @property reason Motivo descriptivo del fallo.
 * @property throwable Excepción original si estuvo disponible.
 */
data class FailedTaskEntry(
    val task: Task,
    val reason: String,
    val throwable: Throwable? = null,
)

/**
 * Guarda las tareas de una [AgentProposal] que el usuario confirmó (F-28).
 *
 * Cada tarea conserva el identificador de su borrador, así que si una escritura falla y el
 * usuario reintenta, las que ya se guardaron se sobrescriben en vez de duplicarse.
 * Captura individualmente el resultado de cada escritura ([SyncStatus]) y retorna un [BatchSaveResult].
 */
class ConfirmAgentProposalUseCase(
    private val upsertTask: UpsertTaskUseCase,
) {

    /**
     * Guarda secuencialmente los borradores de la propuesta, conservando sus identificadores.
     *
     * @param proposal Propuesta con los borradores a guardar.
     * @return [KronoResult.Success] con el [BatchSaveResult], o [KronoResult.Error] si no hay borradores.
     */
    suspend operator fun invoke(proposal: AgentProposal): KronoResult<BatchSaveResult> {
        if (proposal.drafts.isEmpty()) {
            return KronoResult.Error(PlanningAgentError.InvalidRequest("No hay tareas para crear."))
        }
        return saveTasks(proposal.drafts.map { it.toTask() })
    }

    /**
     * Reintenta el guardado de una lista de tareas (por ejemplo, las fallidas de un intento previo),
     * conservando exactamente sus identificadores originales.
     *
     * @param tasks Lista de tareas a reintentar.
     * @return [KronoResult.Success] con el nuevo [BatchSaveResult], o [KronoResult.Error] si la lista está vacía.
     */
    suspend fun retryTasks(tasks: List<Task>): KronoResult<BatchSaveResult> {
        if (tasks.isEmpty()) {
            return KronoResult.Error(PlanningAgentError.InvalidRequest("No hay tareas para reintentar."))
        }
        return saveTasks(tasks)
    }

    private suspend fun saveTasks(tasks: List<Task>): KronoResult<BatchSaveResult> {
        val confirmed = mutableListOf<Task>()
        val pendingSync = mutableListOf<Task>()
        val failed = mutableListOf<FailedTaskEntry>()

        for (task in tasks) {
            when (val result = upsertTask(task)) {
                is KronoResult.Success -> {
                    when (result.data) {
                        SyncStatus.CONFIRMED -> confirmed.add(task.copy(syncStatus = SyncStatus.CONFIRMED))
                        SyncStatus.PENDING_SYNC -> pendingSync.add(task.copy(syncStatus = SyncStatus.PENDING_SYNC))
                        SyncStatus.FAILED -> failed.add(FailedTaskEntry(task, "Fallo de sincronización"))
                        SyncStatus.REJECTED -> failed.add(FailedTaskEntry(task, "Rechazado por el servidor"))
                    }
                }
                is KronoResult.Error -> {
                    val reason = result.throwable.message ?: "Error al guardar la tarea"
                    failed.add(FailedTaskEntry(task = task, reason = reason, throwable = result.throwable))
                }
                KronoResult.Loading -> Unit
            }
        }

        return KronoResult.Success(
            BatchSaveResult(
                confirmedTasks = confirmed,
                pendingSyncTasks = pendingSync,
                failedTasks = failed,
            )
        )
    }
}
