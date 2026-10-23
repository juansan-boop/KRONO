package com.krono.feature.tasks.create.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Selector combinado de fecha y hora límite con diálogos accesibles de Material 3 (F-04 CA-1, CA-2, CA-3).
 *
 * @param dueDate Fecha actualmente seleccionada, o `null`.
 * @param dueTime Hora actualmente seleccionada, o `null`.
 * @param isDateError Indica si hay error en la fecha.
 * @param isTimeError Indica si hay error en la hora.
 * @param errorRes Recurso de texto del error a desplegar si existe.
 * @param onDateSelected Callback al confirmar una nueva fecha.
 * @param onTimeSelected Callback al confirmar una nueva hora.
 * @param modifier Modificador de diseño.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateTimePicker(
    dueDate: LocalDate?,
    dueTime: LocalTime?,
    isDateError: Boolean,
    isTimeError: Boolean,
    @StringRes errorRes: Int?,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = dueTime?.hour ?: DEFAULT_HOUR,
        initialMinute = dueTime?.minute ?: DEFAULT_MINUTE,
        is24Hour = true,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        ) {
            // Columna 1: Fecha límite
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(KronoIcons.CalendarToday),
                        contentDescription = null,
                        tint = PrimaryFixedDim,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(KronoSpacing.xs))
                    Text(
                        text = stringResource(R.string.label_due_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(KronoSpacing.xs))

                val dateBoxBorder = if (isDateError) ErrorColor else OutlineVariant
                val dateText = dueDate?.toString() ?: stringResource(R.string.cd_select_date)
                val dateTextColor = if (dueDate != null) OnSurface else Outline
                val dateShape = RoundedCornerShape(KronoSpacing.md)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.minTouchTarget)
                        .clip(dateShape)
                        .background(SurfaceContainerLow)
                        .border(
                            width = KronoSpacing.xs / 4,
                            color = dateBoxBorder,
                            shape = dateShape,
                        )
                        .clickable(
                            role = Role.Button,
                            onClick = { showDatePicker = true },
                        )
                        .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dateTextColor,
                    )
                }
            }

            // Columna 2: Hora límite
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(KronoSpacing.xs))
                    Text(
                        text = stringResource(R.string.label_due_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(KronoSpacing.xs))

                val timeBoxBorder = if (isTimeError) ErrorColor else OutlineVariant
                val timeText = if (dueTime != null) {
                    stringResource(
                        R.string.due_time_format,
                        dueTime.hour.toString().padStart(2, '0'),
                        dueTime.minute.toString().padStart(2, '0'),
                    )
                } else {
                    stringResource(R.string.cd_select_time)
                }
                val timeTextColor = if (dueTime != null) OnSurface else Outline
                val timeShape = RoundedCornerShape(KronoSpacing.md)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.minTouchTarget)
                        .clip(timeShape)
                        .background(SurfaceContainerLow)
                        .border(
                            width = KronoSpacing.xs / 4,
                            color = timeBoxBorder,
                            shape = timeShape,
                        )
                        .clickable(
                            role = Role.Button,
                            onClick = { showTimePicker = true },
                        )
                        .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = timeTextColor,
                    )
                }
            }
        }

        // Mensaje de error visual debajo de la fila (CA-2, CA-3)
        if ((isDateError || isTimeError) && errorRes != null) {
            Spacer(modifier = Modifier.height(KronoSpacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.Error),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(KronoSpacing.xs))
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor,
                )
            }
        }
    }

    // Diálogo selector de fecha
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val date = instant.toLocalDateTime(TimeZone.UTC).date
                            onDateSelected(date)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_confirm), color = PrimaryFixedDim)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = OnSurfaceVariant)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Diálogo selector de hora
    if (showTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_confirm), color = PrimaryFixedDim)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel), color = OnSurfaceVariant)
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
        )
    }
}

private const val DEFAULT_HOUR = 18
private const val DEFAULT_MINUTE = 0
