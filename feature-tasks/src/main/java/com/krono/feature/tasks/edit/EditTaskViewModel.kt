package com.krono.feature.tasks.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.GetTaskByIdUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.task.UpsertTaskUseCase
import com.krono.feature.tasks.R
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration

/**
 * ViewModel que gestiona la carga, edición y persistencia de una tarea existente (F-05).
 *
 * Cumple estrictamente con el aislamiento de arquitectura: sin imports de `androidx.compose.*`.
 */
@HiltViewModel
class EditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val upsertTaskUseCase: UpsertTaskUseCase,
) : ViewModel() {

    /** Instancia original de la tarea recuperada; se mantiene inmutable para preservar metadatos (CA-2). */
    internal var originalTask: Task? = null
        private set

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(EditTaskUiState())
    val uiState: StateFlow<EditTaskUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditTaskEvent>()
    val events: SharedFlow<EditTaskEvent> = _events.asSharedFlow()

    init {
        val taskId = savedStateHandle.get<String>(TASK_ID_NAV_ARG)
        if (taskId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, isTaskNotFound = true) }
        } else {
            loadTask(taskId)
        }
    }

    /**
     * Consulta la tarea existente por [taskId] y precarga los campos del formulario (CA-1).
     */
    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, taskId = taskId) }
            when (val result = getTaskByIdUseCase(taskId)) {
                is KronoResult.Success -> {
                    val task = result.data
                    originalTask = task
                    _uiState.update { current ->
                        current.copy(
                            taskId = task.id,
                            title = task.title,
                            category = task.category,
                            dueDate = task.dueDate?.date,
                            dueTime = task.dueDate?.time,
                            priority = task.priority,
                            estimatedDuration = task.estimatedDuration,
                            isLoading = false,
                            isTaskNotFound = false,
                            errorMessage = null,
                        )
                    }
                }
                is KronoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isTaskNotFound = true,
                            errorMessage = result.throwable.message,
                        )
                    }
                    val errorMsg = result.throwable.message
                    if (errorMsg != null) {
                        _events.emit(EditTaskEvent.ShowSnackbar(errorMsg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    /**
     * Modifica el título de la tarea y limpia el error previo si existía.
     */
    fun onTitleChanged(newTitle: String) {
        _uiState.update { current ->
            current.copy(
                title = newTitle,
                titleErrorRes = null,
            )
        }
    }

    /**
     * Actualiza la categoría funcional de la tarea.
     */
    fun onCategorySelected(newCategory: TaskCategory) {
        _uiState.update { it.copy(category = newCategory) }
    }

    /**
     * Actualiza la fecha límite de entrega y limpia errores asociados.
     */
    fun onDateSelected(newDate: LocalDate) {
        _uiState.update { current ->
            current.copy(
                dueDate = newDate,
                dateErrorRes = null,
                dateTimeErrorRes = null,
            )
        }
    }

    /**
     * Actualiza la hora límite de entrega y limpia errores de validación temporal.
     */
    fun onTimeSelected(newTime: LocalTime) {
        _uiState.update { current ->
            current.copy(
                dueTime = newTime,
                dateTimeErrorRes = null,
            )
        }
    }

    /**
     * Actualiza la prioridad de la tarea.
     */
    fun onPrioritySelected(newPriority: TaskPriority) {
        _uiState.update { it.copy(priority = newPriority) }
    }

    /**
     * Actualiza la estimación de duración o la elimina (`null`).
     */
    fun onDurationSelected(newDuration: Duration?) {
        _uiState.update { it.copy(estimatedDuration = newDuration) }
    }

    /**
     * Limpia el mensaje de error general en pantalla.
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Valida el formulario y persiste los cambios actualizando los campos editables y preservando
     * intactos los campos no editables (id, isCompleted, actualDuration, subtasks, note) (CA-2, CA-3).
     */
    fun saveChanges() {
        val currentState = _uiState.value
        val original = originalTask
        if (original == null) {
            _uiState.update { it.copy(isTaskNotFound = true) }
            return
        }

        val now = currentDateTimeProvider()
        var hasError = false
        var titleError: Int? = null
        var dateError: Int? = null
        var dateTimeError: Int? = null

        // 1. Validación de título obligatorio (CA-3)
        if (currentState.title.trim().isBlank()) {
            titleError = R.string.error_title_required
            hasError = true
        }

        // 2. Validación de fecha obligatoria (CA-3)
        if (currentState.dueDate == null) {
            dateError = R.string.error_date_required
            hasError = true
        } else {
            // 3. Validación de no retroactividad temporal (CA-3)
            val time = currentState.dueTime ?: LocalTime(23, 59)
            val combinedDateTime = LocalDateTime(currentState.dueDate, time)
            if (combinedDateTime < now) {
                dateTimeError = R.string.error_due_date_in_past
                hasError = true
            }
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    titleErrorRes = titleError,
                    dateErrorRes = dateError,
                    dateTimeErrorRes = dateTimeError,
                )
            }
            return
        }

        val dueDateCombined = currentState.dueDate?.let { date ->
            val time = currentState.dueTime ?: LocalTime(23, 59)
            LocalDateTime(date, time)
        }

        // Preservación estricta de metadatos (CA-2):
        // id, actualDuration, isCompleted, subtasks, note permanecen inalterados.
        val updatedTask = original.copy(
            title = currentState.title.trim(),
            category = currentState.category,
            dueDate = dueDateCombined,
            priority = currentState.priority,
            estimatedDuration = currentState.estimatedDuration,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = upsertTaskUseCase(updatedTask)) {
                is KronoResult.Success -> {
                    originalTask = updatedTask
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(EditTaskEvent.NavigateBack)
                }
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = msg,
                        )
                    }
                    if (msg != null) {
                        _events.emit(EditTaskEvent.ShowSnackbar(msg))
                    }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    companion object {
        /** Nombre del argumento de navegación para el identificador de la tarea. */
        const val TASK_ID_NAV_ARG = "taskId"
    }
}
