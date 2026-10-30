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
import com.krono.feature.tasks.R
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
 * Solo hay una acción en curso a la vez (iniciar, pausar, reanudar o finalizar): los toques que
 * llegan mientras otra sigue guardándose se ignoran, para no escribir ni sumar tiempo dos veces.
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

    /**
     * Finaliza el cronómetro y guarda la sesión en la tarea (CA-1, CA-2). Si la tarea ya no
     * existía, la sesión se conserva sin sumarse y el estado lo indica para avisar al usuario.
     * Los toques repetidos mientras se guarda se ignoran.
     */
    fun onFinish() {
        if (_uiState.value.status == TimerStatus.IDLE || !tryMarkBusy()) return
        viewModelScope.launch {
            when (val result = finishTimer()) {
                is KronoResult.Success -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        savedSession = result.data.session.duration,
                        isSavedSessionWithoutTask = !result.data.addedToTask,
                    )
                }
                is KronoResult.Error -> showActionError()
                KronoResult.Loading -> Unit
            }
        }
    }

    /** Limpia el mensaje de error ya mostrado. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    private fun observeTask(taskId: String) {
        viewModelScope.launch {
            observeTaskById(taskId).collect { result ->
                when (result) {
                    is KronoResult.Success -> _uiState.update {
                        it.copy(isLoading = false, task = result.data, isTaskNotFound = result.data == null)
                    }
                    is KronoResult.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessageRes = R.string.timer_error_load)
                    }
                    KronoResult.Loading -> Unit
                }
            }
        }
    }

    private fun observeTimer(taskId: String) {
        viewModelScope.launch {
            observeActiveTimer()
                .catch { _uiState.update { it.copy(errorMessageRes = R.string.timer_error_load) } }
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
                isSavedSessionWithoutTask = timer == null && it.isSavedSessionWithoutTask,
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
        if (taskId.isNullOrBlank() || !tryMarkBusy()) return
        viewModelScope.launch {
            when (val result = action()) {
                is KronoResult.Success -> _uiState.update { it.copy(isBusy = false) }
                is KronoResult.Error -> handleActionError(result.throwable)
                KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Marca la pantalla como ocupada si no lo estaba, de forma atómica y antes de lanzar la
     * corrutina: devuelve `false` si ya había una acción en curso. Los botones se deshabilitan con
     * [TaskTimerUiState.isBusy], pero varios toques pueden llegar antes de que la pantalla se
     * recomponga, y cada uno lanzaría otra escritura (QA vio el tiempo sumado dos y tres veces).
     */
    private fun tryMarkBusy(): Boolean {
        while (true) {
            val current = _uiState.value
            if (current.isBusy) return false
            if (_uiState.compareAndSet(current, current.copy(isBusy = true))) return true
        }
    }

    private fun handleActionError(error: Throwable) {
        if (error is TimerError.AnotherTimerActive) {
            _uiState.update { it.copy(isBusy = false, isConfirmingSwitch = true) }
        } else {
            showActionError()
        }
    }

    /** Muestra un mensaje genérico: el detalle técnico del error no es útil para el usuario. */
    private fun showActionError() {
        _uiState.update { it.copy(isBusy = false, errorMessageRes = R.string.timer_error_generic) }
    }

    /** Constantes de navegación y de refresco. */
    companion object {
        /** Argumento de navegación con el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"

        /** Cada cuánto se refresca el tiempo en pantalla; el valor sale siempre del reloj. */
        val TICK_INTERVAL = 1.seconds
    }
}
