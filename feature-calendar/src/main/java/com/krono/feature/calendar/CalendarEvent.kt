package com.krono.feature.calendar

import com.krono.core.domain.calendar.CalendarViewMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Eventos de acción y navegación del calendario (F-16).
 *
 * Modela las intenciones del usuario en las vistas mensual, semanal y diaria,
 * incluyendo la creación de tareas con fecha prellenada y la reprogramación directa.
 */
sealed interface CalendarEvent {

    /**
     * Solicita abrir la creación de una tarea con la [initialDate] prellenada.
     *
     * @property initialDate Fecha seleccionada en el calendario para prellenar la nueva tarea.
     */
    data class CreateTask(val initialDate: LocalDate) : CalendarEvent

    /**
     * Solicita reprogramar la tarea identificada por [taskId] a la nueva fecha y hora [newDateTime].
     *
     * @property taskId Identificador único de la tarea a reprogramar.
     * @property newDateTime Nueva fecha y hora límite de entrega asignada.
     */
    data class RescheduleTask(val taskId: String, val newDateTime: LocalDateTime) : CalendarEvent

    /**
     * Solicita abrir el detalle de la tarea identificada por [taskId].
     *
     * @property taskId Identificador único de la tarea.
     */
    data class OpenTask(val taskId: String) : CalendarEvent

    /**
     * Selecciona una fecha en la rejilla o franja para inspeccionar su agenda.
     *
     * @property date Fecha seleccionada.
     */
    data class SelectDate(val date: LocalDate) : CalendarEvent

    /**
     * Cambia la vista activa del calendario (Mes, Semana o Día).
     *
     * @property mode Modo de visualización seleccionado.
     */
    data class SelectMode(val mode: CalendarViewMode) : CalendarEvent

    /** Navega al período anterior según el modo de visualización activo. */
    data object PreviousPeriod : CalendarEvent

    /** Navega al período siguiente según el modo de visualización activo. */
    data object NextPeriod : CalendarEvent

    /** Regresa al día de hoy conservando la vista activa. */
    data object Today : CalendarEvent

    /** Reintenta la observación y carga de tareas tras un fallo. */
    data object Retry : CalendarEvent
}
