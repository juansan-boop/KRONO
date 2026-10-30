package com.krono.feature.tasks.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.ExplainPriorityUseCase
import com.krono.core.domain.assistant.PriorityFactor
import com.krono.core.domain.assistant.PriorityPlan
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
 * ViewModel del asistente de prioridades (F-12, D-5 opción híbrida).
 *
 * Recalcula el plan con la heurística local cada vez que cambian las tareas, así que la
 * recomendación aparece al instante y sin conexión. Solo cuando cambia la tarea recomendada
 * o sus motivos le pide a la IA una explicación nueva, para no repetir llamadas al modelo.
 *
 * Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*`.
 */
@HiltViewModel
class PriorityAssistantViewModel @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val buildPriorityPlan: BuildPriorityPlanUseCase,
    private val explainPriority: ExplainPriorityUseCase,
) : ViewModel() {

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(PriorityAssistantUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<PriorityAssistantUiState> = _uiState.asStateFlow()

    private var latestTasks: List<Task> = emptyList()
    private var explainedRecommendation: RecommendationKey? = null
    private var explanationJob: Job? = null

    init {
        observeTasks()
    }

    /**
     * Recalcula el plan con la hora actual y pide de nuevo la explicación. Si la lectura de
     * tareas había fallado, vuelve a suscribirse.
     */
    fun onRefresh() {
        explainedRecommendation = null
        if (_uiState.value.hasError) {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            observeTasks()
        } else {
            updatePlan(latestTasks)
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            observeTasksUseCase().collect { result ->
                when (result) {
                    is KronoResult.Success -> {
                        latestTasks = result.data
                        updatePlan(result.data)
                    }
                    is KronoResult.Error -> _uiState.update { it.copy(isLoading = false, hasError = true) }
                    KronoResult.Loading -> Unit
                }
            }
        }
    }

    private fun updatePlan(tasks: List<Task>) {
        val plan = buildPriorityPlan(tasks, currentDateTimeProvider())
        _uiState.update { it.copy(isLoading = false, hasError = false, plan = plan) }
        if (plan == null) {
            cancelExplanation()
            return
        }
        val key = RecommendationKey(plan.recommended.task.id, plan.recommended.factors)
        if (key != explainedRecommendation) {
            explainedRecommendation = key
            requestExplanation(plan)
        }
    }

    private fun requestExplanation(plan: PriorityPlan) {
        explanationJob?.cancel()
        _uiState.update { it.copy(explanation = ExplanationState.Loading) }
        explanationJob = viewModelScope.launch {
            val explanation = when (val result = explainPriority(plan)) {
                is KronoResult.Success -> ExplanationState.Generated(result.data)
                is KronoResult.Error, KronoResult.Loading -> ExplanationState.Fallback
            }
            _uiState.update { it.copy(explanation = explanation) }
        }
    }

    private fun cancelExplanation() {
        explanationJob?.cancel()
        explainedRecommendation = null
    }

    /** Identifica una recomendación: si no cambia, la explicación anterior sigue siendo válida. */
    private data class RecommendationKey(val taskId: String, val factors: Set<PriorityFactor>)
}
