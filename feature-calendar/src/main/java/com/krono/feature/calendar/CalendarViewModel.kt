package com.krono.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CalendarPeriod
import com.krono.core.domain.calendar.CalendarSchedule
import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.calendar.ObserveCalendarUseCase
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel de la vista mensual del calendario (F-16 CA-1 y CA-2): mes mostrado, día
 * seleccionado y agenda de ese día.
 *
 * Solo vuelve a observar las tareas cuando cambia el mes o se reintenta; seleccionar un día del
 * mismo mes reutiliza la agenda ya cargada. Mientras la pantalla está visible recalcula el estado
 * cada [CLOCK_REFRESH_INTERVAL] para que "En curso" y "hoy" sigan al reloj. Nunca importa
 * `androidx.compose.*`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeCalendar: ObserveCalendarUseCase,
    private val dateTimeProvider: CurrentDateTimeProvider,
) : ViewModel() {

    private val selection = MutableStateFlow(selectionOfToday())

    /** Cada incremento vuelve a abrir la observación de tareas tras un fallo. */
    private val loadAttempts = MutableStateFlow(0)

    private val loadedMonth: Flow<LoadedMonth> =
        combine(selection.map { it.period }.distinctUntilChanged(), loadAttempts) { period, _ -> period }
            .flatMapLatest { period ->
                observeCalendar(period)
                    .onStart { emit(KronoResult.Loading) }
                    .map { result -> LoadedMonth(period, result) }
            }

    /**
     * Pulso que obliga a recalcular el estado con la hora actual. Vive solo mientras alguien
     * observa [uiState] (pantalla visible), así que no consume batería en segundo plano; al
     * volver a primer plano emite de inmediato.
     */
    private val clockTicks: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(CLOCK_REFRESH_INTERVAL)
        }
    }.onEach { followTodayIfDayChanged() }

    /** Último "hoy" visto por el pulso, para detectar el cambio de día. */
    private var lastSeenToday = dateTimeProvider.today()

    /** Estado observable por la pantalla. */
    val uiState: StateFlow<CalendarUiState> = combine(loadedMonth, selection, clockTicks) { loaded, current, _ ->
        buildState(current, loaded.resultFor(current.period))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildState(selection.value, KronoResult.Loading),
    )

    /** Muestra el mes anterior y selecciona hoy si cae en él o, si no, su día 1. */
    fun onPreviousMonth() = showMonth(selection.value.period.previous())

    /** Muestra el mes siguiente y selecciona hoy si cae en él o, si no, su día 1. */
    fun onNextMonth() = showMonth(selection.value.period.next())

    /**
     * Selecciona [date] para ver su agenda (F-16 CA-2). Un día de un mes vecino de la rejilla
     * cambia el mes mostrado al suyo.
     */
    fun onSelectDate(date: LocalDate) {
        selection.update { current ->
            val period = if (current.period.isInAnchorMonth(date)) current.period else monthOf(date)
            MonthSelection(period, date)
        }
    }

    /** Vuelve al mes actual con el día de hoy seleccionado. */
    fun onToday() {
        selection.value = selectionOfToday()
    }

    /** Vuelve a abrir la observación de tareas después de un error de carga. */
    fun onRetry() {
        loadAttempts.update { it + 1 }
    }

    /**
     * Al cambiar el día (pasada la medianoche) lleva la selección a la nueva fecha de hoy, pero
     * solo si el usuario seguía en "hoy"; si miraba otro día, se respeta su elección.
     */
    private fun followTodayIfDayChanged() {
        val today = dateTimeProvider.today()
        if (today == lastSeenToday) return
        val previousToday = lastSeenToday
        lastSeenToday = today
        if (selection.value.selectedDate == previousToday) selection.value = selectionOfToday()
    }

    private fun showMonth(period: CalendarPeriod) {
        val today = dateTimeProvider.today()
        val selected = if (period.isInAnchorMonth(today)) today else period.anchor
        selection.value = MonthSelection(period, selected)
    }

    private fun selectionOfToday(): MonthSelection {
        val today = dateTimeProvider.today()
        return MonthSelection(monthOf(today), today)
    }

    private fun buildState(current: MonthSelection, result: KronoResult<CalendarSchedule>): CalendarUiState =
        CalendarStateBuilder(
            period = current.period,
            selectedDate = current.selectedDate,
            today = dateTimeProvider.today(),
            now = dateTimeProvider.now(),
        ).build(result)

    /** Mes y día seleccionados, que siempre cambian juntos para no mostrar estados mezclados. */
    private data class MonthSelection(val period: CalendarPeriod, val selectedDate: LocalDate)

    /** Resultado de observar [period]. */
    private data class LoadedMonth(val period: CalendarPeriod, val result: KronoResult<CalendarSchedule>) {
        // Tras cambiar de mes la agenda anterior sigue aquí hasta que llega la nueva: se trata como
        // carga para no mostrar un día del mes nuevo sin tareas por error.
        fun resultFor(shown: CalendarPeriod): KronoResult<CalendarSchedule> =
            if (shown == period) result else KronoResult.Loading
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        // El ancla se normaliza al día 1 para que el mismo mes sea siempre el mismo período.
        fun monthOf(date: LocalDate) = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(date.year, date.month, 1))
    }
}

/**
 * Cada cuánto se recalcula el estado mientras la pantalla está visible, para que "En curso" y
 * "hoy" no se queden obsoletos con la pantalla quieta.
 */
internal val CLOCK_REFRESH_INTERVAL = 30.seconds
