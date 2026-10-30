package com.krono.feature.tasks.focusmode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.focus.GetActiveFocusTemplateUseCase
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.FocusDefaults
import com.krono.core.domain.timer.ObserveActiveTimerUseCase
import com.krono.core.domain.timer.PauseTimerUseCase
import com.krono.core.domain.timer.ResumeTimerUseCase
import com.krono.core.domain.timer.StartFocusBreakUseCase
import com.krono.core.domain.timer.StartFocusUseCase
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
 * ViewModel del modo enfoque de una tarea (F-09).
 *
 * Al abrirse inicia un enfoque con la plantilla activa (CA-1); si ya hay uno en curso para la tarea,
 * lo muestra sin tocarlo. La cuenta regresiva se recalcula cada segundo desde las marcas de tiempo
 * del enfoque guardado, nunca restando ticks, así que sobrevive a segundo plano y a cerrar la app.
 *
 * Solo hay una acción en curso a la vez: los toques que llegan mientras otra se guarda se ignoran,
 * para no escribir ni sumar tiempo dos veces.
 *
 * Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*`.
 */
@HiltViewModel
class FocusModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTaskById: ObserveTaskByIdUseCase,
    private val observeActiveTimer: ObserveActiveTimerUseCase,
    private val getActiveFocusTemplate: GetActiveFocusTemplateUseCase,
    private val startFocus: StartFocusUseCase,
    private val startFocusBreak: StartFocusBreakUseCase,
    private val pauseTimer: PauseTimerUseCase,
    private val resumeTimer: ResumeTimerUseCase,
    private val finishTimer: FinishTimerUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>(TASK_ID_NAV_ARG)

    private val _uiState = MutableStateFlow(FocusModeUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<FocusModeUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var currentTimer: ActiveTimer? = null

    /** El enfoque se inicia una sola vez por pantalla: tras finalizar no debe reiniciarse solo. */
    private var hasStartedFocus = false

    init {
        if (taskId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, isTaskNotFound = true) }
        } else {
            observeTask(taskId)
            observeTimer(taskId)
        }
    }

    /** Pausa la cuenta regresiva (CA-2). */
    fun onPause() = runAction { pauseTimer() }

    /** Reanuda el trabajo tras una pausa o un descanso (CA-2). */
    fun onResume() = runAction { resumeTimer() }

    /** Empieza el descanso corto del botón "+5m Descanso"; el trabajo se detiene y no cuenta (CA-2). */
    fun onExtraBreak() = runAction { startFocusBreak(FocusDefaults.extraBreak) }

    /** Empieza el descanso de la plantilla que se ofrece al llegar a cero (CA-3). */
    fun onTakeTemplateBreak() = runAction { startFocusBreak(_uiState.value.templateBreak) }

    /**
     * Finaliza el enfoque y guarda la sesión `ENFOQUE` con el tiempo realmente trabajado (CA-2). Si
     * la tarea ya no existía, la sesión se conserva sin sumarse y el estado lo indica. Los toques
     * repetidos mientras se guarda se ignoran.
     */
    fun onFinish() {
        if (_uiState.value.phase == null || !tryMarkBusy()) return
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

    /** Confirma detener la sesión de otra tarea, guardando su tiempo, e iniciar este enfoque. */
    fun onConfirmSwitch() {
        _uiState.update { it.copy(isConfirmingSwitch = false) }
        runAction { startFocus(requireNotNull(taskId), getActiveFocusTemplate(), stopCurrent = true) }
    }

    /** Descarta el cambio y deja corriendo la sesión de la otra tarea. */
    fun onDismissSwitch() {
        _uiState.update { it.copy(isConfirmingSwitch = false) }
    }

    /** Limpia el mensaje de error ya mostrado. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    private fun observeTask(taskId: String) {
        viewModelScope.launch {
            observeTaskById(taskId).collect { result ->
                when (result) {
                    is KronoResult.Success -> onTaskChanged(result.data)
                    is KronoResult.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessageRes = R.string.focus_mode_error_load)
                    }
                    KronoResult.Loading -> Unit
                }
            }
        }
    }

    /** Una tarea que desaparece con la pantalla abierta se conserva para poder guardar la sesión. */
    private fun onTaskChanged(task: Task?) {
        _uiState.update { state ->
            when {
                task != null -> state.copy(isLoading = false, task = task, isTaskDeleted = false, isTaskNotFound = false)
                state.task != null -> state.copy(isLoading = false, isTaskDeleted = true)
                else -> state.copy(isLoading = false, isTaskNotFound = true)
            }
        }
        if (task != null && !hasStartedFocus) {
            hasStartedFocus = true
            runAction { startFocus(task.id, getActiveFocusTemplate()) }
        }
    }

    private fun observeTimer(taskId: String) {
        viewModelScope.launch {
            observeActiveTimer()
                .catch { _uiState.update { it.copy(errorMessageRes = R.string.focus_mode_error_load) } }
                .collect { timer -> onTimerChanged(timer?.takeIf { it.taskId == taskId && it.focus != null }) }
        }
    }

    private fun onTimerChanged(timer: ActiveTimer?) {
        tickJob?.cancel()
        currentTimer = timer
        renderTimer()
        if (timer != null) tickJob = startTicking()
    }

    private fun startTicking(): Job = viewModelScope.launch {
        while (isActive) {
            delay(TICK_INTERVAL)
            renderTimer()
        }
    }

    /** Vuelca al estado lo que el reloj dice del enfoque guardado en este instante. */
    private fun renderTimer() {
        val timer = currentTimer
        val now = clock.now()
        _uiState.update { state ->
            state.copy(
                phase = timer?.focusPhaseAt(now),
                remaining = timer?.focusRemainingAt(now) ?: Duration.ZERO,
                blockDuration = timer?.focus?.blockDuration ?: Duration.ZERO,
                breakRemaining = timer?.breakRemainingAt(now),
                breakDuration = timer?.focus?.breakInProgress?.duration,
                templateBreak = timer?.focus?.breakDuration ?: state.templateBreak,
                // Al empezar un enfoque nuevo, el resumen del anterior ya no aplica.
                savedSession = if (timer != null) null else state.savedSession,
                isSavedSessionWithoutTask = timer == null && state.isSavedSessionWithoutTask,
            )
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
     * corrutina: devuelve `false` si ya había una acción en curso. Varios toques pueden llegar antes
     * de que la pantalla se recomponga y cada uno lanzaría otra escritura.
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
        _uiState.update { it.copy(isBusy = false, errorMessageRes = R.string.focus_mode_error_generic) }
    }

    /** Constantes de navegación y de refresco. */
    companion object {
        /** Argumento de navegación con el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"

        /** Cada cuánto se refresca la pantalla; el valor sale siempre del reloj. */
        val TICK_INTERVAL = 1.seconds
    }
}
