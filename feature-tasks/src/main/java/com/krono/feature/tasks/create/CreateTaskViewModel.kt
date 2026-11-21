package com.krono.feature.tasks.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.task.ObserveTasksUseCase
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
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel que gestiona el estado y la lógica de validación para la creación de tareas (F-04).
 *
 * Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*`.
 */
@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val upsertTaskUseCase: UpsertTaskUseCase,
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val estimateTaskDuration: EstimateTaskDurationUseCase,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    /** Proveedor del instante actual; modificable en pruebas unitarias para aislar el tiempo. */
    internal var currentDateTimeProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    /** Estado observable de la pantalla. */
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateTaskEvent>()
    /** Eventos de una sola vez (navegación y mensajes). */
    val events: SharedFlow<CreateTaskEvent> = _events.asSharedFlow()

    /** Historial de tareas del usuario, base de la sugerencia de duración (F-11). */
    private var taskHistory: List<Task> = emptyList()

    init {
        val restoredTitle = savedStateHandle.get<String>(KEY_DRAFT_TITLE)
        val restoredNote = savedStateHandle.get<String>(KEY_DRAFT_NOTE)
        val restoredCategory = savedStateHandle.get<String>(KEY_DRAFT_CATEGORY)?.let { name ->
            runCatching { TaskCategory.valueOf(name) }.getOrNull()
        }
        val restoredPriority = savedStateHandle.get<String>(KEY_DRAFT_PRIORITY)?.let { name ->
            runCatching { TaskPriority.valueOf(name) }.getOrNull()
        }
        val restoredDueDate = savedStateHandle.get<String>(KEY_DRAFT_DUE_DATE)?.let { dateStr ->
            runCatching { LocalDate.parse(dateStr) }.getOrNull()
        } ?: savedStateHandle.get<String>(INITIAL_DATE_ARG)?.let { dateStr ->
            runCatching { LocalDate.parse(dateStr) }.getOrNull()
        }
        val restoredDueTime = savedStateHandle.get<String>(KEY_DRAFT_DUE_TIME)?.let { timeStr ->
            runCatching { LocalTime.parse(timeStr) }.getOrNull()
        }
        val restoredDuration = savedStateHandle.get<Long>(KEY_DRAFT_ESTIMATED_DURATION)?.let { secs ->
            secs.seconds
        }

        _uiState.update { current ->
            current.copy(
                title = restoredTitle ?: current.title,
                note = restoredNote ?: current.note,
                category = restoredCategory ?: current.category,
                priority = restoredPriority ?: current.priority,
                dueDate = restoredDueDate ?: current.dueDate,
                dueTime = restoredDueTime ?: current.dueTime,
                estimatedDuration = restoredDuration ?: current.estimatedDuration,
            )
        }

        observeTaskHistory()
    }

    /**
     * Actualiza el título de la tarea y limpia errores previos del campo.
     */
    fun onTitleChanged(newTitle: String) {
        savedStateHandle[KEY_DRAFT_TITLE] = newTitle
        _uiState.update { current ->
            current.copy(
                title = newTitle,
                titleErrorRes = null,
            )
        }
    }

    /**
     * Actualiza la nota del borrador de la tarea.
     */
    fun onNoteChanged(newNote: String) {
        savedStateHandle[KEY_DRAFT_NOTE] = newNote
        _uiState.update { it.copy(note = newNote) }
    }

    /**
     * Selecciona la categoría de la tarea.
     */
    fun onCategorySelected(newCategory: TaskCategory) {
        savedStateHandle[KEY_DRAFT_CATEGORY] = newCategory.name
        _uiState.update { it.copy(category = newCategory) }
        refreshDurationSuggestion()
    }

    /**
     * Selecciona la fecha límite de entrega y limpia errores asociados.
     */
    fun onDateSelected(newDate: LocalDate) {
        savedStateHandle[KEY_DRAFT_DUE_DATE] = newDate.toString()
        _uiState.update { current ->
            current.copy(
                dueDate = newDate,
                dateErrorRes = null,
                dateTimeErrorRes = null,
            )
        }
        refreshDurationSuggestion()
    }

    /**
     * Selecciona la hora límite de entrega y limpia errores asociados.
     */
    fun onTimeSelected(newTime: LocalTime) {
        savedStateHandle[KEY_DRAFT_DUE_TIME] = newTime.toString()
        _uiState.update { current ->
            current.copy(
                dueTime = newTime,
                dateTimeErrorRes = null,
            )
        }
        refreshDurationSuggestion()
    }

    /**
     * Selecciona el nivel de prioridad de la tarea.
     */
    fun onPrioritySelected(newPriority: TaskPriority) {
        savedStateHandle[KEY_DRAFT_PRIORITY] = newPriority.name
        _uiState.update { it.copy(priority = newPriority) }
        refreshDurationSuggestion()
    }

    /**
     * Selecciona la estimación de duración o `null` si se deselecciona (CA-4).
     */
    fun onDurationSelected(newDuration: Duration?) {
        if (newDuration != null) {
            savedStateHandle[KEY_DRAFT_ESTIMATED_DURATION] = newDuration.inWholeSeconds
        } else {
            savedStateHandle.remove<Long>(KEY_DRAFT_ESTIMATED_DURATION)
        }
        _uiState.update { it.copy(estimatedDuration = newDuration) }
    }

    /**
     * Aplica con un toque la duración sugerida por historial, si existe (F-11 CA-1).
     */
    fun onApplyDurationSuggestion() {
        _uiState.value.durationSuggestion?.let(::onDurationSelected)
    }

    /**
     * Limpia el mensaje de error general activo.
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Valida el formulario y persiste la tarea si cumple todos los criterios (CA-1, CA-2, CA-3, CA-4).
     * Mientras hay un guardado en curso las llamadas se ignoran: cada guardado genera un
     * identificador nuevo, así que un doble toque duplicaría la tarea.
     */
    fun saveTask() {
        val currentState = _uiState.value
        if (currentState.isSaving) return
        val now = currentDateTimeProvider()

        var hasError = false
        var titleError: Int? = null
        var dateError: Int? = null
        var dateTimeError: Int? = null

        // 1. Validación de título obligatorio (CA-2)
        if (currentState.title.trim().isBlank()) {
            titleError = R.string.error_title_required
            hasError = true
        }

        // 2. Validación de fecha obligatoria (CA-2)
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

        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = currentState.title.trim(),
            note = currentState.note.trim().takeIf { it.isNotEmpty() },
            category = currentState.category,
            priority = currentState.priority,
            dueDate = dueDateCombined,
            estimatedDuration = currentState.estimatedDuration,
            actualDuration = Duration.ZERO,
            isCompleted = false,
        )

        // Se marca antes de lanzar la corrutina para que un segundo toque ya lo vea.
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = upsertTaskUseCase(newTask)) {
                is KronoResult.Success -> {
                    clearDraft()
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(CreateTaskEvent.NavigateBack)
                }
                is KronoResult.Error -> {
                    val msg = result.throwable.message
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = msg,
                        )
                    }
                    msg?.let { _events.emit(CreateTaskEvent.ShowSnackbar(it)) }
                }
                is KronoResult.Loading -> Unit
            }
        }
    }

    // Sin historial la sugerencia simplemente no aparece (F-11 CA-2), así que un fallo al
    // leer las tareas no se muestra como error en el formulario.
    private fun observeTaskHistory() {
        viewModelScope.launch {
            observeTasksUseCase().collect { result ->
                taskHistory = (result as? KronoResult.Success)?.data.orEmpty()
                refreshDurationSuggestion()
            }
        }
    }

    private fun refreshDurationSuggestion() {
        _uiState.update { current ->
            val hasMeasuredEstimatedHistory = taskHistory.count {
                it.isCompleted &&
                    !it.isDeleted &&
                    it.category == current.category &&
                    it.actualDuration > Duration.ZERO &&
                    it.estimatedDuration?.isPositive() == true
            } >= 3

            val suggestion = if (hasMeasuredEstimatedHistory) {
                val now = currentDateTimeProvider()
                val daysUntilDue = current.dueDate?.let { date ->
                    val time = current.dueTime ?: LocalTime(23, 59)
                    val dueDateTime = LocalDateTime(date, time)
                    val diffMinutes = (dueDateTime.toInstant(TimeZone.UTC) - now.toInstant(TimeZone.UTC)).inWholeMinutes
                    (diffMinutes.toDouble() / 1440.0).coerceAtLeast(0.0)
                } ?: 2.0
                val hourOfDay = current.dueTime?.hour ?: now.hour

                estimateTaskDuration.predictHybrid(
                    category = current.category,
                    tasks = taskHistory,
                    priority = current.priority,
                    daysUntilDue = daysUntilDue,
                    hourOfDay = hourOfDay,
                ) ?: estimateTaskDuration(current.category, taskHistory)
            } else {
                estimateTaskDuration(current.category, taskHistory)
            }

            current.copy(durationSuggestion = suggestion)
        }
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(KEY_DRAFT_TITLE)
        savedStateHandle.remove<String>(KEY_DRAFT_NOTE)
        savedStateHandle.remove<String>(KEY_DRAFT_CATEGORY)
        savedStateHandle.remove<String>(KEY_DRAFT_PRIORITY)
        savedStateHandle.remove<String>(KEY_DRAFT_DUE_DATE)
        savedStateHandle.remove<String>(KEY_DRAFT_DUE_TIME)
        savedStateHandle.remove<Long>(KEY_DRAFT_ESTIMATED_DURATION)
    }

    companion object {
        /** Argumento de navegación opcional con la fecha inicial en formato ISO (YYYY-MM-DD). */
        const val INITIAL_DATE_ARG = "initialDate"

        /** Claves de persistencia de borrador en [SavedStateHandle]. */
        const val KEY_DRAFT_TITLE = "draft_title"
        const val KEY_DRAFT_NOTE = "draft_note"
        const val KEY_DRAFT_CATEGORY = "draft_category"
        const val KEY_DRAFT_PRIORITY = "draft_priority"
        const val KEY_DRAFT_DUE_DATE = "draft_due_date"
        const val KEY_DRAFT_DUE_TIME = "draft_due_time"
        const val KEY_DRAFT_ESTIMATED_DURATION = "draft_estimated_duration_seconds"
    }
}
