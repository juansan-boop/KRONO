package com.krono.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.ObserveTasksUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskField
import com.krono.core.domain.task.UpdateTaskFieldsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import javax.inject.Inject

/**
 * ViewModel que gestiona la lógica de presentación y el estado reactivo de la lista de tareas (F-03).
 *
 * Regla dura de arquitectura: este ViewModel nunca importa `androidx.compose.*` y expone
 * su estado exclusivamente mediante [StateFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val updateTaskFieldsUseCase: UpdateTaskFieldsUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModel() {

    /** Proveedor de la fecha actual; modificable en pruebas unitarias para aislar el tiempo. */
    internal var currentDateProvider: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    }

    private val selectedCategory = MutableStateFlow(TaskCategoryFilter.TODAS)
    private val taskPendingDelete = MutableStateFlow<Task?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private var confirmedDeleteJob: Job? = null

    /** Cada incremento vuelve a abrir la observación de tareas tras un fallo. */
    private val loadAttempts = MutableStateFlow(0)

    private val tasksResult: Flow<KronoResult<List<Task>>> = loadAttempts.flatMapLatest {
        observeTasksUseCase().onStart { emit(KronoResult.Loading) }
    }

    /** Estado reactivo observable por la interfaz de usuario. */
    val uiState: StateFlow<TasksUiState> = combine(
        tasksResult,
        selectedCategory,
        taskPendingDelete,
        errorMessage,
    ) { result, categoryFilter, pendingDelete, error ->
        when (result) {
            KronoResult.Loading -> TasksUiState(isLoading = true, selectedCategory = categoryFilter)
            is KronoResult.Error -> TasksUiState(
                isLoading = false,
                today = currentDateProvider(),
                selectedCategory = categoryFilter,
                hasLoadError = true,
                errorMessage = error,
            )
            is KronoResult.Success -> buildLoadedState(result.data, categoryFilter, pendingDelete, error)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TasksUiState(isLoading = true),
    )

    /**
     * Cambia la categoría seleccionada para filtrar la lista de tareas (CA-2).
     */
    fun onSelectCategory(filter: TaskCategoryFilter) {
        selectedCategory.value = filter
    }

    /**
     * Vuelve a abrir la observación de tareas después de un error de carga.
     */
    fun onRetryLoad() {
        loadAttempts.update { it + 1 }
    }

    /**
     * Conmuta el estado de completitud de una tarea y persiste solo ese campo (CA-3), para no
     * pisar el tiempo real si la tarea de la lista está desactualizada.
     */
    fun onToggleTaskCompletion(task: Task) {
        launchWrite {
            updateTaskFieldsUseCase(task.copy(isCompleted = !task.isCompleted), setOf(TaskField.IS_COMPLETED))
        }
    }

    /**
     * Solicita la eliminación de una tarea abriendo el diálogo de confirmación (F-07 CA-1).
     *
     * @param task Tarea a eliminar junto a sus subtareas asociadas.
     */
    fun onRequestDeleteTask(task: Task) {
        taskPendingDelete.value = task
    }

    /**
     * Cierra el diálogo de confirmación de eliminación conservando la tarea (F-07 CA-2).
     */
    fun onDismissDeleteDialog() {
        taskPendingDelete.value = null
    }

    /**
     * Confirma la eliminación de la tarea pendiente de confirmación y sus subtareas (F-07 CA-2).
     * Un segundo toque mientras el borrado sigue en curso se ignora.
     */
    fun onConfirmDeleteTask() {
        val task = taskPendingDelete.value ?: return
        if (confirmedDeleteJob?.isActive == true) return
        confirmedDeleteJob = launchWrite {
            taskPendingDelete.value = null
            deleteTaskUseCase(task.id)
        }
    }

    /**
     * Pospone la tarea para el día de mañana sumando exactamente un día a su fecha límite (F-07 CA-3).
     * Si la tarea no tiene fecha límite previa, calcula mañana deterministamente con [currentDateProvider].
     * Solo se escribe la fecha límite.
     *
     * @param task Tarea a posponer; si es nula, toma la tarea pendiente en [taskPendingDelete].
     */
    fun onPostponeTaskToTomorrow(task: Task? = null) {
        val targetTask = task ?: taskPendingDelete.value ?: return
        val postponed = targetTask.copy(dueDate = tomorrowDueDate(targetTask.dueDate))
        launchWrite {
            taskPendingDelete.value = null
            updateTaskFieldsUseCase(postponed, setOf(TaskField.DUE_DATE))
        }
    }

    /**
     * Elimina una tarea y sus subtareas asociadas directamente.
     */
    fun onDeleteTask(taskId: String) {
        launchWrite { deleteTaskUseCase(taskId) }
    }

    /**
     * Limpia el mensaje de error activo.
     */
    fun clearError() {
        errorMessage.value = null
    }

    private fun buildLoadedState(
        allTasks: List<Task>,
        categoryFilter: TaskCategoryFilter,
        pendingDelete: Task?,
        error: String?,
    ): TasksUiState {
        val today = currentDateProvider()

        // Enfoque Diario se calcula con las tareas de hoy sin filtrar por chip (CA-3)
        val allTodayTasks = allTasks.filter { TaskTimeClassifier.isToday(it, today) }

        // Filtrar tareas según la categoría activa (CA-2)
        val domainCategory = categoryFilter.toDomainCategory()
        val filteredTasks = if (domainCategory == null) allTasks else allTasks.filter { it.category == domainCategory }

        return TasksUiState(
            isLoading = false,
            today = today,
            selectedCategory = categoryFilter,
            categoryCounts = countByFilter(allTasks),
            dailyFocusPercentage = DailyFocusCalculator.calculatePercentage(allTodayTasks),
            dailyPendingCount = DailyFocusCalculator.countPending(allTodayTasks),
            // Agrupar en "Para Hoy" y "Esta Semana" (CA-1)
            todayTasks = filteredTasks.filter { TaskTimeClassifier.isToday(it, today) },
            thisWeekTasks = filteredTasks.filter { TaskTimeClassifier.isThisWeek(it, today) },
            taskPendingDelete = pendingDelete,
            errorMessage = error,
        )
    }

    /** Conteo de tareas por filtro para los chips de la cabecera. */
    private fun countByFilter(allTasks: List<Task>): Map<TaskCategoryFilter, Int> =
        TaskCategoryFilter.entries.associateWith { filter ->
            val domainCategory = filter.toDomainCategory()
            if (domainCategory == null) allTasks.size else allTasks.count { it.category == domainCategory }
        }

    private fun tomorrowDueDate(currentDueDate: LocalDateTime?): LocalDateTime =
        if (currentDueDate != null) {
            LocalDateTime(currentDueDate.date.plus(1, DateTimeUnit.DAY), currentDueDate.time)
        } else {
            LocalDateTime(currentDateProvider().plus(1, DateTimeUnit.DAY), DEFAULT_DUE_TIME)
        }

    /** Ejecuta una escritura y refleja su resultado en el mensaje de error. */
    private fun launchWrite(write: suspend () -> KronoResult<Unit>): Job =
        viewModelScope.launch {
            when (val result = write()) {
                is KronoResult.Error -> errorMessage.value = result.throwable.message
                is KronoResult.Success -> errorMessage.value = null
                KronoResult.Loading -> Unit
            }
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val DEFAULT_DUE_TIME = LocalTime(23, 59)
    }
}
