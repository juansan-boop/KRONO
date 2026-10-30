package com.krono.feature.calendar

import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Estado inmutable del calendario en cualquiera de sus tres vistas (F-16 CA-1, CA-2 y CA-3).
 *
 * @property mode Vista activa: mes, semana o día.
 * @property today Fecha actual, resaltada en la rejilla o la franja.
 * @property selectedDate Día cuya agenda se muestra. Es el hilo común entre vistas: al cambiar de
 * vista se conserva, y el mes o la semana mostrados son siempre los que lo contienen.
 * @property weekNumber Número de semana ISO de [selectedDate] (lunes a domingo), para los títulos
 * de Semana y Día.
 * @property weeks En Mes, la rejilla de semanas completas con días de los meses vecinos; en
 * Semana y Día, una sola semana de lunes a domingo (la franja de días).
 * @property agenda Estado de la agenda del día seleccionado: cargando, error o tareas.
 */
data class CalendarUiState(
    val mode: CalendarViewMode,
    val today: LocalDate,
    val selectedDate: LocalDate,
    val weekNumber: Int,
    val weeks: List<List<CalendarDayUi>>,
    val agenda: DayAgendaState,
)

/**
 * Celda de un día de la rejilla mensual o de la franja semanal.
 *
 * @property date Fecha de la celda.
 * @property isInDisplayedPeriod `false` solo para los días de los meses vecinos de la rejilla
 * mensual (se muestran atenuados); en Semana y Día siempre es `true`.
 * @property isToday Indica si es la fecha actual.
 * @property isSelected Indica si es el día cuya agenda se muestra.
 * @property taskCount Tareas del día, o `null` mientras no se conocen (cargando o error): así la
 * celda no anuncia "sin tareas" cuando en realidad no se pudieron leer.
 * @property categories Categorías distintas de las tareas del día, en orden de agenda, hasta
 * [MAX_DAY_INDICATORS] (un punto por categoría).
 */
data class CalendarDayUi(
    val date: LocalDate,
    val isInDisplayedPeriod: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val taskCount: Int?,
    val categories: List<TaskCategory>,
)

/** Estado de la agenda del día seleccionado. */
sealed interface DayAgendaState {

    /** Las tareas todavía se están leyendo. */
    data object Loading : DayAgendaState

    /**
     * No se pudieron leer las tareas (por ejemplo, permiso denegado). La pantalla ofrece
     * "Reintentar" en vez de una agenda vacía falsa. Sin conexión no es un error: la caché de
     * Firestore sigue sirviendo las tareas.
     */
    data object LoadError : DayAgendaState

    /**
     * Tareas del día en el orden del dominio (inicio y prioridad).
     *
     * @property items Tareas del día; vacía si el día no tiene ninguna.
     * @property nowMarker Separador "Ahora" de la vista diaria, o `null` fuera de ella.
     */
    data class Loaded(val items: List<AgendaItemUi>, val nowMarker: NowMarker? = null) : DayAgendaState
}

/**
 * Separador "Ahora · HH:mm" de la vista diaria (F-16 CA-3): marca dónde cae la hora actual dentro
 * de la agenda de hoy.
 *
 * @property beforeIndex Posición en la lista de tareas delante de la que se dibuja: la de la
 * primera tarea que aún no terminó, o el tamaño de la lista si todas terminaron.
 * @property time Hora actual que muestra el separador.
 */
data class NowMarker(val beforeIndex: Int, val time: LocalDateTime)

/**
 * Tarea de la agenda del día, con su horario derivado (D-14).
 *
 * @property taskId Identificador de la tarea, para abrir su detalle.
 * @property title Título de la tarea.
 * @property note Nota de la tarea, o `null` si no tiene.
 * @property start Inicio del bloque: la fecha límite menos la estimación.
 * @property end Fin del bloque: la fecha límite.
 * @property category Categoría de la tarea (color del punto).
 * @property priority Prioridad de la tarea.
 * @property status Estado mostrado: programada, en curso o completada.
 */
data class AgendaItemUi(
    val taskId: String,
    val title: String,
    val note: String?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val category: TaskCategory,
    val priority: TaskPriority,
    val status: AgendaItemStatus,
)

/** Estado de una tarea de la agenda. */
enum class AgendaItemStatus {
    /** Pendiente y fuera de su intervalo. */
    SCHEDULED,

    /** Pendiente y su intervalo incluye la hora actual (F-16 CA-3). */
    IN_PROGRESS,

    /** Ya completada; nunca se marca "En curso". */
    COMPLETED,
}

/** Máximo de puntos de categoría bajo un día de la rejilla. */
const val MAX_DAY_INDICATORS = 3
