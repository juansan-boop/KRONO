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
 * ViewModel del calendario en sus tres vistas, Mes, Semana y Día (F-16 CA-1, CA-2 y CA-3): la
 * vista activa, el día seleccionado y la agenda de ese día.
 *
 * El estado de navegación es solo la pareja (vista, día seleccionado): el mes o la semana
 * mostrados salen del día, así que cambiar de vista conserva el día sin lógica extra. Semana y Día
 * observan la misma semana, por lo que alternar entre ellas o recorrer sus días no vuelve a
 * leer las tareas; solo cambiar de mes o de semana, o reintentar, lo hace. Mientras la pantalla
 * está visible recalcula el estado cada [CLOCK_REFRESH_INTERVAL] para que "En curso", el separador
 * "Ahora" y "hoy" sigan al reloj. Nunca importa `androidx.compose.*`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeCalendar: ObserveCalendarUseCase,
    private val dateTimeProvider: CurrentDateTimeProvider,
) : ViewModel() {

    private val selection = MutableStateFlow(selectionOfToday(CalendarViewMode.MONTH))

    /** Cada incremento vuelve a abrir la observación de tareas tras un fallo. */
    private val loadAttempts = MutableStateFlow(0)

    private val loadedSchedule: Flow<LoadedSchedule> =
        combine(selection.map { it.dataPeriod }.distinctUntilChanged(), loadAttempts) { period, _ -> period }
            .flatMapLatest { period ->
                observeCalendar(period)
                    .onStart { emit(KronoResult.Loading) }
                    .map { result -> LoadedSchedule(period, result) }
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
    val uiState: StateFlow<CalendarUiState> = combine(loadedSchedule, selection, clockTicks) { loaded, current, _ ->
        buildState(current, loaded.resultFor(current.dataPeriod))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildState(selection.value, KronoResult.Loading),
    )

    /**
     * Cambia a la vista [mode] conservando el día seleccionado: la nueva vista muestra el mes, la
     * semana o el día que lo contiene.
     */
    fun onSelectMode(mode: CalendarViewMode) {
        selection.update { it.copy(mode = mode) }
    }

    /** Muestra el período anterior (mes, semana o día, según la vista) y selecciona su día de referencia. */
    fun onPrevious() = showPeriod(selection.value.navigationPeriod.previous())

    /** Muestra el período siguiente (mes, semana o día, según la vista) y selecciona su día de referencia. */
    fun onNext() = showPeriod(selection.value.navigationPeriod.next())

    /**
     * Selecciona [date] para ver su agenda (F-16 CA-2). Un día de un mes vecino de la rejilla
     * cambia el mes mostrado al suyo; en Semana y Día, un día de la franja cambia el día.
     */
    fun onSelectDate(date: LocalDate) {
        selection.update { it.copy(selectedDate = date) }
    }

    /** Vuelve al período actual con el día de hoy seleccionado, sin cambiar de vista. */
    fun onToday() {
        selection.update { selectionOfToday(it.mode) }
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
        selection.update { if (it.selectedDate == previousToday) it.copy(selectedDate = today) else it }
    }

    /**
     * Selecciona hoy si cae en [period]; si no, su día de referencia ([CalendarPeriod.anchor]: el
     * día 1 del mes, o el mismo día de la semana o el día en que se aterrizó).
     */
    private fun showPeriod(period: CalendarPeriod) {
        val today = dateTimeProvider.today()
        val selected = if (period.shows(today)) today else period.anchor
        selection.update { it.copy(selectedDate = selected) }
    }

    private fun selectionOfToday(mode: CalendarViewMode) = ViewSelection(mode, dateTimeProvider.today())

    private fun buildState(current: ViewSelection, result: KronoResult<CalendarSchedule>): CalendarUiState =
        CalendarStateBuilder(
            mode = current.mode,
            period = current.dataPeriod,
            selectedDate = current.selectedDate,
            today = dateTimeProvider.today(),
            now = dateTimeProvider.now(),
        ).build(result)

    /**
     * Vista y día seleccionado, que siempre cambian juntos para no mostrar estados mezclados.
     * Semana y Día cargan la misma semana: la vista diaria necesita la franja de su semana.
     */
    private data class ViewSelection(val mode: CalendarViewMode, val selectedDate: LocalDate) {

        /** Período sobre el que navegan "anterior" y "siguiente". */
        val navigationPeriod: CalendarPeriod
            get() = if (mode == CalendarViewMode.MONTH) monthOf(selectedDate) else CalendarPeriod(mode, selectedDate)

        /** Período cuyas tareas se observan, normalizado para que el mismo período sea siempre igual. */
        val dataPeriod: CalendarPeriod
            get() = if (mode == CalendarViewMode.MONTH) monthOf(selectedDate) else weekOf(selectedDate)
    }

    /** Resultado de observar [period]. */
    private data class LoadedSchedule(val period: CalendarPeriod, val result: KronoResult<CalendarSchedule>) {
        // Tras cambiar de período la agenda anterior sigue aquí hasta que llega la nueva: se trata
        // como carga para no mostrar un día del período nuevo sin tareas por error.
        fun resultFor(shown: CalendarPeriod): KronoResult<CalendarSchedule> =
            if (shown == period) result else KronoResult.Loading
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        // El ancla se normaliza al día 1 para que el mismo mes sea siempre el mismo período.
        fun monthOf(date: LocalDate) = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(date.year, date.month, 1))

        // El ancla se normaliza al lunes para que la misma semana sea siempre el mismo período.
        fun weekOf(date: LocalDate) =
            CalendarPeriod(CalendarViewMode.WEEK, CalendarPeriod(CalendarViewMode.WEEK, date).firstDay)

        // En Mes cuenta solo el mes propio, no los días vecinos que la rejilla también muestra.
        fun CalendarPeriod.shows(date: LocalDate) =
            if (mode == CalendarViewMode.MONTH) isInAnchorMonth(date) else contains(date)
    }
}

/**
 * Cada cuánto se recalcula el estado mientras la pantalla está visible, para que "En curso" y
 * "hoy" no se queden obsoletos con la pantalla quieta.
 */
internal val CLOCK_REFRESH_INTERVAL = 30.seconds
