package com.krono.feature.tasks.ui

import android.content.Context
import com.krono.feature.tasks.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/**
 * Utilidades para formatear fechas y duraciones de tareas en la interfaz gráfica.
 */
object TaskDateFormatter {

    /**
     * Formatea la fecha y hora límite de entrega en una cadena amigable en español.
     * Si la fecha coincide con [today], formatea como "Hoy, HH:mm hrs".
     */
    fun formatDueDate(context: Context, dueDate: LocalDateTime?, today: LocalDate): String {
        if (dueDate == null) {
            return context.getString(R.string.no_due_date)
        }
        val timeString = String.format("%02d:%02d", dueDate.hour, dueDate.minute)
        return if (dueDate.date == today) {
            context.getString(R.string.due_today_format, timeString)
        } else {
            val dateString = String.format("%02d/%02d", dueDate.dayOfMonth, dueDate.monthNumber)
            context.getString(R.string.due_time_format, dateString, timeString)
        }
    }

    /**
     * Formatea una duración estimada a formato de texto legible (ej. "~30 min est.").
     */
    fun formatEstimatedDuration(context: Context, duration: Duration?): String? {
        if (duration == null) return null
        val totalMinutes = duration.inWholeMinutes
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60

        val durationString = when {
            hours > 0 && remainingMinutes > 0 ->
                context.getString(R.string.duration_hours_minutes, hours, remainingMinutes)
            hours > 0 ->
                context.getString(R.string.duration_hours, hours)
            else ->
                context.getString(R.string.duration_minutes, totalMinutes)
        }
        return context.getString(R.string.estimated_duration_format, durationString)
    }

    /**
     * Formatea el tiempo restante calculado en una cadena amigable en español (F-06 CA-1).
     */
    fun formatRemainingTime(context: Context, remainingTime: com.krono.feature.tasks.detail.RemainingTime): String = when (remainingTime) {
        is com.krono.feature.tasks.detail.RemainingTime.Overdue ->
            context.getString(R.string.task_detail_overdue)
        is com.krono.feature.tasks.detail.RemainingTime.InMinutes ->
            if (remainingTime.minutes == 1L) {
                context.getString(R.string.task_detail_remaining_single_minute)
            } else {
                context.getString(R.string.task_detail_remaining_minutes, remainingTime.minutes)
            }
        is com.krono.feature.tasks.detail.RemainingTime.InHours ->
            if (remainingTime.hours == 1L) {
                context.getString(R.string.task_detail_remaining_single_hour)
            } else {
                context.getString(R.string.task_detail_remaining_hours, remainingTime.hours)
            }
        is com.krono.feature.tasks.detail.RemainingTime.InDays ->
            if (remainingTime.days == 1L) {
                context.getString(R.string.task_detail_remaining_single_day)
            } else {
                context.getString(R.string.task_detail_remaining_days, remainingTime.days)
            }
    }
}
