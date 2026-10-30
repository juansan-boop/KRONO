package com.krono.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.ObserveTaskByIdUseCase
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.UpdateTaskFieldsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * Cada escritura toca solo el campo que cambia (subtareas o fecha límite), de modo que nunca
 * pisa el tiempo real que suma el cronómetro (F-08).
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTaskByIdUseCase: ObserveTaskByIdUseCase,
    private val updateTaskFieldsUseCase: UpdateTaskFieldsUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModel() {

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo deterministamente. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    /** Estado observable de la pantalla. */
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>()
    /** Eventos de una sola vez (navegación y mensajes). */
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    private var observedTaskId: String? = null
    private var observeJob: Job? = null

    /** Borrado o posposición en curso; mientras siga activo se ignoran nuevos toques. */
    private var dueDateOrDeleteJob: Job? = null

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
     * Si ya había una observación (por ejemplo, una que falló), la reemplaza.
     */
    fun observeTask(taskId: String) {
        observedTaskId = taskId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasLoadError = false) }
            observeTaskByIdUseCase(taskId).collect { result ->
                when (result) {
                    is KronoResult.Success -> showTask(result.data)
                    is KronoResult.Error -> _uiState.update {
                        it.copy(isLoading = false, hasLoadError = true, task = null, remainingTime = null)
                    }
                    KronoResult.Loading -> Unit
                }
            }
        }
    }

    /**
     * Vuelve a abrir la observación de la tarea después de un error de carga.
     */
    fun onRetryLoad() {
        observedTaskId?.let(::observeTask)
    }

    /**
     * Conmuta el estado de completitud de una subtarea (`isDone`) y persiste el cambio (CA-2).
     */
    fun onToggleSubtask(subtaskId: String) {
        val currentTask = _uiState.value.task ?: return
        val updatedSubtasks = currentTask.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(isDone = !subtask.isDone) else subtask
        }
        viewModelScope.launch {
            writeSubtasks(currentTask.copy(subtasks = updatedSubtasks))
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
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSubtask = true, isAddingSubtask = false) }
            writeSubtasks(currentTask.copy(subtasks = currentTask.subtasks + newSubtask))
            _uiState.update { it.copy(isSavingSubtask = false) }
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
     * Un segundo toque mientras el borrado sigue en curso se ignora, para no navegar dos veces.
     */
    fun onConfirmDeleteTask() {
        val taskId = _uiState.value.task?.id ?: return
        if (dueDateOrDeleteJob?.isActive == true) return
        _uiState.update { it.copy(isConfirmingDelete = false) }
        dueDateOrDeleteJob = viewModelScope.launch {
            when (val result = deleteTaskUseCase(taskId)) {
                is KronoResult.Success -> _events.emit(TaskDetailEvent.NavigateBack)
                is KronoResult.Error -> showWriteError(result.throwable)
                KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Pospone la tarea para el día de mañana sumando un día a la fecha límite sin eliminarla (F-07 CA-3).
     * Si la tarea no tiene fecha límite previa, calcula mañana deterministamente con [currentDateTimeProvider].
     * Solo se escribe la fecha límite, y un segundo toque en curso se ignora para no sumar dos días.
     */
    fun onPostponeTaskToTomorrow() {
        val currentTask = _uiState.value.task ?: return
        if (dueDateOrDeleteJob?.isActive == true) return
        val postponed = currentTask.copy(dueDate = tomorrowDueDate(currentTask.dueDate))
        _uiState.update { it.copy(isConfirmingDelete = false) }
        dueDateOrDeleteJob = viewModelScope.launch {
            val result = updateTaskFieldsUseCase(postponed, setOf(TaskField.DUE_DATE))
            if (result is KronoResult.Error) showWriteError(result.throwable)
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

    private fun showTask(task: Task?) {
        if (task == null) {
            _uiState.update {
                it.copy(isLoading = false, isTaskNotFound = true, task = null, remainingTime = null)
            }
            return
        }
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

    // La lista de subtareas se escribe completa porque vive embebida en el documento (D-3).
    private suspend fun writeSubtasks(task: Task) {
        val result = updateTaskFieldsUseCase(task, setOf(TaskField.SUBTASKS))
        if (result is KronoResult.Error) showWriteError(result.throwable)
    }

    private suspend fun showWriteError(error: Throwable) {
        val message = error.message
        _uiState.update { it.copy(errorMessage = message) }
        if (message != null) {
            _events.emit(TaskDetailEvent.ShowSnackbar(message))
        }
    }

    private fun tomorrowDueDate(currentDueDate: LocalDateTime?): LocalDateTime =
        if (currentDueDate != null) {
            LocalDateTime(currentDueDate.date.plus(1, DateTimeUnit.DAY), currentDueDate.time)
        } else {
            LocalDateTime(currentDateTimeProvider().date.plus(1, DateTimeUnit.DAY), DEFAULT_DUE_TIME)
        }

    companion object {
        /** Argumento canónico de navegación para el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"

        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24

        /** Hora límite que se asigna al posponer una tarea que no tenía fecha. */
        val DEFAULT_DUE_TIME = LocalTime(23, 59)
    }
}
