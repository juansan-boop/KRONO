package com.krono.feature.tasks.assistant.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.agent.AgentProposal
import com.krono.core.domain.assistant.agent.AskPlanningAgentUseCase
import com.krono.core.domain.assistant.agent.ConfirmAgentProposalUseCase
import com.krono.core.domain.assistant.agent.PlanningAgentError
import com.krono.core.domain.task.ObserveTasksUseCase
import com.krono.core.domain.task.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * ViewModel del agente de planificación (F-28): envía el mensaje del usuario a Gemini, muestra
 * la propuesta (tareas estimadas por H1 y plan de H2) y solo guarda las tareas cuando el
 * usuario confirma.
 *
 * Al guardarlas, el asistente de prioridades se recalcula solo porque observa las mismas
 * tareas. Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*`.
 */
@HiltViewModel
class PlanningAgentViewModel @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val askPlanningAgent: AskPlanningAgentUseCase,
    private val confirmAgentProposal: ConfirmAgentProposalUseCase,
) : ViewModel() {

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(PlanningAgentUiState())

    /** Estado observable de la sección del agente. */
    val uiState: StateFlow<PlanningAgentUiState> = _uiState.asStateFlow()

    // Historial para H1 y tareas actuales para H2; si la lectura falla se planifica sin ellas.
    private var latestTasks: List<Task> = emptyList()
    private var requestJob: Job? = null

    init {
        viewModelScope.launch {
            observeTasksUseCase().collect { result ->
                if (result is KronoResult.Success) latestTasks = result.data
            }
        }
    }

    /** Actualiza el mensaje, recortado al largo máximo que acepta el agente. */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text.take(AskPlanningAgentUseCase.MAX_REQUEST_LENGTH)) }
    }

    /** Envía el mensaje al agente; una propuesta anterior sin confirmar se reemplaza. */
    fun onSend() {
        val state = _uiState.value
        if (!state.canSend) return
        _uiState.update { it.copy(status = AgentStatus.Thinking) }
        requestJob = viewModelScope.launch {
            val result = askPlanningAgent(state.input, latestTasks, currentDateTimeProvider())
            _uiState.update { it.copy(status = statusOf(result)) }
        }
    }

    /** Guarda las tareas de la propuesta actual. */
    fun onConfirm() {
        val proposal = (_uiState.value.status as? AgentStatus.Proposed)?.proposal ?: return
        if (proposal.drafts.isEmpty()) return
        _uiState.update { it.copy(status = AgentStatus.Saving(proposal)) }
        viewModelScope.launch {
            when (val result = confirmAgentProposal(proposal)) {
                is KronoResult.Success -> _uiState.update { PlanningAgentUiState(status = AgentStatus.Saved(result.data)) }
                is KronoResult.Error, KronoResult.Loading -> _uiState.update {
                    it.copy(status = AgentStatus.Proposed(proposal, saveFailed = true))
                }
            }
        }
    }

    /** Descarta la propuesta o el resultado y cancela la petición en curso, si la hay. */
    fun onDiscard() {
        requestJob?.cancel()
        _uiState.update { it.copy(status = AgentStatus.Idle) }
    }

    private fun statusOf(result: KronoResult<AgentProposal>): AgentStatus = when (result) {
        is KronoResult.Success -> AgentStatus.Proposed(result.data)
        is KronoResult.Error -> AgentStatus.Failed(failureOf(result.throwable))
        KronoResult.Loading -> AgentStatus.Thinking
    }

    private fun failureOf(error: Throwable): AgentFailure = when (error) {
        is PlanningAgentError.InvalidRequest -> AgentFailure.INVALID_REQUEST
        PlanningAgentError.NothingProposed -> AgentFailure.NOT_UNDERSTOOD
        else -> AgentFailure.UNAVAILABLE
    }
}
