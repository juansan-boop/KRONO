package com.krono.feature.tasks.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.ObserveActiveTimerUseCase
import com.krono.core.domain.timer.PauseTimerUseCase
import com.krono.core.domain.timer.ResumeTimerUseCase
import com.krono.core.domain.timer.StartTimerUseCase
import com.krono.core.domain.timer.TimerError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel del cronómetro de una tarea (F-08).
 *
 * El tiempo mostrado se recalcula cada segundo desde las marcas de tiempo del cronómetro
 * guardado, nunca sumando ticks: si la app pasa a segundo plano o se cierra, al volver el
 * tiempo es el correcto (CA-3).
 *
 * Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*`.
 */
@HiltViewModel
class TaskTimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTaskById: ObserveTaskByIdUseCase,
    private val observeActiveTimer: ObserveActiveTimerUseCase,
    private val startTimer: StartTimerUseCase,
    private val pauseTimer: PauseTimerUseCase,
    private val resumeTimer: ResumeTimerUseCase,
    private val finishTimer: FinishTimerUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>(TASK_ID_NAV_ARG)

    private val _uiState = MutableStateFlow(TaskTimerUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<TaskTimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        if (taskId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, isTaskNotFound = true) }
        } else {
            observeTask(taskId)
            observeTimer(taskId)
        }
    }

    /** Inicia el cronómetro; si hay otro en curso pide confirmación (CA-1, CA-4). */
    fun onStart() = runAction { startTimer(requireNotNull(taskId)) }

    /** Confirma detener el cronómetro de otra tarea, guardando su tiempo, e iniciar este (CA-4). */
    fun onConfirmSwitch() {
        _uiState.update { it.copy(isConfirmingSwitch = false) }
        runAction { startTimer(requireNotNull(taskId), stopCurrent = true) }
    }

    /** Descarta el cambio de cronómetro y deja corriendo el de la otra tarea. */
    fun onDismissSwitch() {
        _uiState.update { it.copy(isConfirmingSwitch = false) }
    }

    /** Pausa el cronómetro (CA-1). */
    fun onPause() = runAction { pauseTimer() }

    /** Reanuda el cronómetro (CA-1). */
    fun onResume() = runAction { resumeTimer() }

    /** Finaliza el cronómetro y guarda la sesión en la tarea (CA-1, CA-2). */
    fun onFinish() {
        if (_uiState.value.status == TimerStatus.IDLE) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (val result = finishTimer()) {
                is KronoResult.Success -> _uiState.update {
                    it.copy(isBusy = false, savedSession = result.data.duration)
                }
                is KronoResult.Error -> showError(result.throwable)
                KronoResult.Loading -> Unit
            }
        }
    }

    /** Limpia el mensaje de error ya mostrado. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeTask(taskId: String) {
        viewModelScope.launch {
            observeTaskById(taskId).collect { task ->
                _uiState.update { it.copy(isLoading = false, task = task, isTaskNotFound = task == null) }
            }
        }
    }

    private fun observeTimer(taskId: String) {
        viewModelScope.launch {
            observeActiveTimer()
                .catch { showError(it) }
                .collect { timer -> onTimerChanged(timer?.takeIf { it.taskId == taskId }) }
        }
    }

    /** Actualiza el estado con el cronómetro de esta tarea, o `null` si no lo hay. */
    private fun onTimerChanged(timer: ActiveTimer?) {
        tickJob?.cancel()
        val status = when {
            timer == null -> TimerStatus.IDLE
            timer.isRunning -> TimerStatus.RUNNING
            else -> TimerStatus.PAUSED
        }
        _uiState.update {
            it.copy(
                status = status,
                elapsed = timer?.elapsedAt(clock.now()) ?: Duration.ZERO,
                // Al empezar una sesión nueva, el resumen de la anterior ya no aplica.
                savedSession = if (timer != null) null else it.savedSession,
            )
        }
        if (timer != null && timer.isRunning) tickJob = startTicking(timer)
    }

    private fun startTicking(timer: ActiveTimer): Job = viewModelScope.launch {
        while (isActive) {
            delay(TICK_INTERVAL)
            _uiState.update { it.copy(elapsed = timer.elapsedAt(clock.now())) }
        }
    }

    private fun runAction(action: suspend () -> KronoResult<Unit>) {
        if (taskId.isNullOrBlank()) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (val result = action()) {
                is KronoResult.Success -> _uiState.update { it.copy(isBusy = false) }
                is KronoResult.Error -> handleActionError(result.throwable)
                KronoResult.Loading -> Unit
            }
        }
    }

    private fun handleActionError(error: Throwable) {
        if (error is TimerError.AnotherTimerActive) {
            _uiState.update { it.copy(isBusy = false, isConfirmingSwitch = true) }
        } else {
            showError(error)
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update { it.copy(isBusy = false, errorMessage = error.message) }
    }

    /** Constantes de navegación y de refresco. */
    companion object {
        /** Argumento de navegación con el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"

        /** Cada cuánto se refresca el tiempo en pantalla; el valor sale siempre del reloj. */
        val TICK_INTERVAL = 1.seconds
    }
}
