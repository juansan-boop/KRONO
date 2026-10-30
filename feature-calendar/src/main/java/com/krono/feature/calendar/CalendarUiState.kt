package com.krono.feature.calendar

import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Estado inmutable de la vista mensual del calendario (F-16 CA-1 y CA-2).
 *
 * @property displayedMonth Cualquier fecha del mes mostrado; la pantalla solo usa su año y mes
 * para el título.
 * @property today Fecha actual, resaltada en la rejilla.
 * @property selectedDate Día cuya agenda se muestra debajo de la rejilla.
 * @property weeks Rejilla del mes: semanas completas de lunes a domingo, con días de los meses
 * vecinos.
 * @property agenda Estado de la agenda del día seleccionado: cargando, error o tareas.
 */
data class CalendarUiState(
    val displayedMonth: LocalDate,
    val today: LocalDate,
    val selectedDate: LocalDate,
    val weeks: List<List<CalendarDayUi>>,
    val agenda: DayAgendaState,
)

/**
 * Celda de un día de la rejilla mensual.
 *
 * @property date Fecha de la celda.
 * @property isInDisplayedMonth `false` para los días de los meses vecinos (se muestran atenuados).
 * @property isToday Indica si es la fecha actual.
 * @property isSelected Indica si es el día cuya agenda se muestra.
 * @property taskCount Tareas del día, o `null` mientras no se conocen (cargando o error): así la
 * celda no anuncia "sin tareas" cuando en realidad no se pudieron leer.
 * @property categories Categorías distintas de las tareas del día, en orden de agenda, hasta
 * [MAX_DAY_INDICATORS] (un punto por categoría).
 */
data class CalendarDayUi(
    val date: LocalDate,
    val isInDisplayedMonth: Boolean,
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
     */
    data class Loaded(val items: List<AgendaItemUi>) : DayAgendaState
}

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
