package com.krono.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.UpsertTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel que gobierna la visualización y gestión del detalle de tarea y sus subtareas (F-06, F-07).
 *
 * Cumple estrictamente con el aislamiento de arquitectura: sin imports de `androidx.compose.*`.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTaskByIdUseCase: ObserveTaskByIdUseCase,
    private val upsertTaskUseCase: UpsertTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModel() {

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo deterministamente. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>()
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    init {
        val taskId = savedStateHandle.get<String>(TASK_ID_NAV_ARG)
        if (taskId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, isTaskNotFound = true) }
        } else {
            observeTask(taskId)
        }
    }

    /**
     * Observa en tiempo real los cambios en la tarea y actualiza el estado y el tiempo restante (CA-1).
     */
    fun observeTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            observeTaskByIdUseCase(taskId).collect { task ->
                if (task == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isTaskNotFound = true,
                            task = null,
                            remainingTime = null,
                        )
                    }
                } else {
                    val remaining = calculateRemainingTime(task.dueDate, currentDateTimeProvider())
                    _uiState.update {
                        it.copy(
                            task = task,
                            remainingTime = remaining,
                            isLoading = false,
                            isTaskNotFound = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Conmuta el estado de completitud de una subtarea (`isDone`) y persiste el cambio (CA-2).
     */
    fun onToggleSubtask(subtaskId: String) {
        val currentTask = _uiState.value.task ?: return
        val updatedSubtasks = currentTask.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(isDone = !subtask.isDone) else subtask
        }
        val updatedTask = currentTask.copy(subtasks = updatedSubtasks)
        viewModelScope.launch {
            when (val result = upsertTaskUseCase(updatedTask)) {
                is KronoResult.Success -> Unit
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update { it.copy(errorMessage = msg) }
                    if (msg != null) {
                        _events.emit(TaskDetailEvent.ShowSnackbar(msg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Abre el diálogo para añadir una nueva subtarea.
     */
    fun onOpenAddSubtaskDialog() {
        _uiState.update { it.copy(isAddingSubtask = true) }
    }

    /**
     * Cierra el diálogo para añadir una nueva subtarea.
     */
    fun onDismissAddSubtaskDialog() {
        _uiState.update { it.copy(isAddingSubtask = false) }
    }

    /**
     * Añade una nueva subtarea a la lista embebida de la tarea y persiste el cambio (CA-2).
     */
    fun onAddSubtask(title: String) {
        if (title.trim().isBlank()) {
            return
        }
        val currentTask = _uiState.value.task ?: return
        val newSubtask = Subtask(
            id = UUID.randomUUID().toString(),
            taskId = currentTask.id,
            title = title.trim(),
            isDone = false,
            position = currentTask.subtasks.size,
        )
        val updatedTask = currentTask.copy(subtasks = currentTask.subtasks + newSubtask)
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSubtask = true, isAddingSubtask = false) }
            when (val result = upsertTaskUseCase(updatedTask)) {
                is KronoResult.Success -> {
                    _uiState.update { it.copy(isSavingSubtask = false) }
                }
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update {
                        it.copy(
                            isSavingSubtask = false,
                            errorMessage = msg,
                        )
                    }
                    if (msg != null) {
                        _events.emit(TaskDetailEvent.ShowSnackbar(msg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Solicita la navegación a la pantalla de edición de tarea (F-05).
     */
    fun onEditTaskClicked() {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _events.emit(TaskDetailEvent.NavigateToEdit(taskId))
        }
    }

    /**
     * Solicita iniciar el modo enfoque para la tarea actual (F-09 CA-3).
     */
    fun onStartFocusClicked() {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _events.emit(TaskDetailEvent.NavigateToFocus(taskId))
        }
    }

    /**
     * Abre el diálogo de confirmación para eliminar la tarea actual (F-07 CA-1).
     */
    fun onDeleteTaskClicked() {
        _uiState.update { it.copy(isConfirmingDelete = true) }
    }

    /**
     * Cierra el diálogo de confirmación de eliminación conservando la tarea (F-07 CA-2).
     */
    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(isConfirmingDelete = false) }
    }

    /**
     * Confirma la eliminación de la tarea actual y sus subtareas, navegando hacia atrás (F-07 CA-2).
     */
    fun onConfirmDeleteTask() {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirmingDelete = false) }
            when (val result = deleteTaskUseCase(taskId)) {
                is KronoResult.Success -> {
                    _events.emit(TaskDetailEvent.NavigateBack)
                }
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update { it.copy(errorMessage = msg) }
                    if (msg != null) {
                        _events.emit(TaskDetailEvent.ShowSnackbar(msg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Pospone la tarea para el día de mañana sumando un día a la fecha límite sin eliminarla (F-07 CA-3).
     * Si la tarea no tiene fecha límite previa, calcula mañana deterministamente con [currentDateTimeProvider].
     */
    fun onPostponeTaskToTomorrow() {
        val currentTask = _uiState.value.task ?: return
        val currentDueDate = currentTask.dueDate
        val newDueDate = if (currentDueDate != null) {
            val nextDate = currentDueDate.date.plus(1, DateTimeUnit.DAY)
            LocalDateTime(nextDate, currentDueDate.time)
        } else {
            val today = currentDateTimeProvider().date
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            LocalDateTime(tomorrow, DEFAULT_DUE_TIME)
        }
        val updatedTask = currentTask.copy(dueDate = newDueDate)
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirmingDelete = false) }
            when (val result = upsertTaskUseCase(updatedTask)) {
                is KronoResult.Success -> Unit
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update { it.copy(errorMessage = msg) }
                    if (msg != null) {
                        _events.emit(TaskDetailEvent.ShowSnackbar(msg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Limpia el mensaje de error general mostrado.
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Función pura que calcula el tiempo restante entre la fecha de entrega y el instante actual (CA-1).
     */
    internal fun calculateRemainingTime(dueDate: LocalDateTime?, now: LocalDateTime): RemainingTime? {
        if (dueDate == null) return null
        val dueInstant = dueDate.toInstant(TimeZone.UTC)
        val nowInstant = now.toInstant(TimeZone.UTC)
        val diff = dueInstant - nowInstant
        return when {
            diff.isNegative() -> RemainingTime.Overdue
            diff.inWholeMinutes < MINUTES_PER_HOUR -> RemainingTime.InMinutes(maxOf(1L, diff.inWholeMinutes))
            diff.inWholeHours < HOURS_PER_DAY -> RemainingTime.InHours(diff.inWholeHours)
            else -> RemainingTime.InDays(diff.inWholeDays)
        }
    }

    companion object {
        /** Argumento canónico de navegación para el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"

        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24
        val DEFAULT_DUE_TIME = LocalTime(23, 59)
    }
}
