package com.krono.feature.calendar.month.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.Tertiary
import com.krono.feature.calendar.R

/**
 * Color de acento de cada categoría en el calendario (puntos de la rejilla y de la agenda),
 * tomado de la paleta del diseño: magenta para estudio, cian para trabajo y rosa para personal.
 */
internal val TaskCategory.accentColor: Color
    get() = when (this) {
        TaskCategory.ESTUDIO -> PrimaryFixedDim
        TaskCategory.TRABAJO -> SecondaryCyan
        TaskCategory.PERSONAL -> Tertiary
    }

/** Nombre visible de la categoría. */
@get:StringRes
internal val TaskCategory.labelRes: Int
    get() = when (this) {
        TaskCategory.ESTUDIO -> R.string.calendar_category_study
        TaskCategory.TRABAJO -> R.string.calendar_category_work
        TaskCategory.PERSONAL -> R.string.calendar_category_personal
    }

/** Texto visible de la prioridad ("Prioridad alta"). */
@get:StringRes
internal val TaskPriority.labelRes: Int
    get() = when (this) {
        TaskPriority.ALTA -> R.string.calendar_priority_high
        TaskPriority.MEDIA -> R.string.calendar_priority_medium
        TaskPriority.BAJA -> R.string.calendar_priority_low
    }
