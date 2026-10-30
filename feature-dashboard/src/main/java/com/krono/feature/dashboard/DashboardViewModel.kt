package com.krono.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.dashboard.DashboardSummary
import com.krono.core.domain.dashboard.ObserveDashboardUseCase
import com.krono.core.domain.profile.ObserveUserNameUseCase
import com.krono.core.domain.task.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel de la pantalla de inicio (F-13 CA-1, CA-3 y CA-4): saludo, avance del día, tiempo de
 * enfoque y tareas prioritarias. Obtiene todo vía casos de uso de `core-domain`, sin conocer
 * `feature-tasks`. Nunca importa `androidx.compose.*`.
 *
 * Mientras la pantalla está visible comprueba cada [DAY_CHECK_INTERVAL] si cambió el día, para
 * que pasada la medianoche el resumen deje de contar el día anterior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
    private val observeUserName: ObserveUserNameUseCase,
    private val dateTimeProvider: CurrentDateTimeProvider,
) : ViewModel() {

    /** Cada incremento vuelve a abrir la observación tras un fallo. */
    private val loadAttempts = MutableStateFlow(0)

    private val currentDay: Flow<LocalDate> = flow {
        while (true) {
            emit(dateTimeProvider.today())
            delay(DAY_CHECK_INTERVAL)
        }
    }.distinctUntilChanged()

    private val summary: Flow<KronoResult<DashboardSummary>> =
        combine(currentDay, loadAttempts) { day, _ -> day }
            .flatMapLatest { day -> observeDashboard(day).onStart { emit(KronoResult.Loading) } }

    // El nombre es decorativo: mientras no llega (o no existe) se saluda sin él.
    private val greetingName: Flow<String?> = observeUserName().onStart { emit(null) }

    /** Estado observable por la pantalla. */
    val uiState: StateFlow<DashboardUiState> = combine(summary, greetingName, ::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState.Loading,
        )

    /** Vuelve a leer las tareas y las sesiones después de un error. */
    fun onRetry() {
        loadAttempts.update { it + 1 }
    }

    private fun buildState(result: KronoResult<DashboardSummary>, userName: String?): DashboardUiState =
        when (result) {
            KronoResult.Loading -> DashboardUiState.Loading
            is KronoResult.Error -> DashboardUiState.Error
            is KronoResult.Success -> result.data.toContent(userName)
        }

    private fun DashboardSummary.toContent(userName: String?) = DashboardUiState.Content(
        userName = userName,
        completedPercent = completedPercent,
        completedCount = completedCount,
        pendingCount = pendingCount,
        focusTime = focusTime,
        hasTasksToday = hasTasksToday,
        priorityTasks = priorityTasks.take(MAX_PRIORITY_TASKS).map { it.toUi() },
    )

    private fun Task.toUi() = PriorityTaskUi(
        taskId = id,
        title = title,
        dueTime = dueDate?.time,
        category = category,
        priority = priority,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Cada cuánto se comprueba si cambió el día mientras la pantalla está visible. */
internal val DAY_CHECK_INTERVAL = 30.seconds
