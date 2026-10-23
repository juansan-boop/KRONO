package com.krono.feature.tasks.create.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.feature.tasks.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Selector de chips para estimación de duración opcional de la tarea (F-04 CA-1, CA-4).
 *
 * @param selectedDuration Duración seleccionada actualmente, o `null` si no hay estimación.
 * @param onDurationSelected Callback invocado al seleccionar o deseleccionar una duración.
 * @param modifier Modificador de diseño.
 */
@Composable
fun DurationSelector(
    selectedDuration: Duration?,
    onDurationSelected: (Duration?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(KronoSpacing.xs))
                Text(
                    text = stringResource(R.string.label_estimated_duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.estimated_duration_optional),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(KronoSpacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            val durationOptions = listOf(
                25.minutes to stringResource(R.string.duration_chip_25m),
                45.minutes to stringResource(R.string.duration_chip_45m),
                60.minutes to stringResource(R.string.duration_chip_1h),
            )

            durationOptions.forEach { (duration, label) ->
                val isSelected = selectedDuration == duration
                DurationChipButton(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        // Toggling: si ya está seleccionado, se deselecciona a null (CA-4)
                        if (isSelected) onDurationSelected(null) else onDurationSelected(duration)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Opción "Otro..." para duración personalizada
            val isCustomSelected = selectedDuration != null &&
                selectedDuration != 25.minutes &&
                selectedDuration != 45.minutes &&
                selectedDuration != 60.minutes

            val otherLabel = if (isCustomSelected) {
                stringResource(R.string.duration_minutes_short, selectedDuration!!.inWholeMinutes)
            } else {
                stringResource(R.string.duration_chip_other)
            }

            DurationChipButton(
                label = otherLabel,
                isSelected = isCustomSelected,
                onClick = { showCustomDialog = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showCustomDialog) {
        var customInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.custom_duration_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            inputError = false
                        },
                        placeholder = { Text(stringResource(R.string.custom_duration_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError,
                    )
                    if (inputError) {
                        Spacer(modifier = Modifier.height(KronoSpacing.xs))
                        Text(
                            text = stringResource(R.string.error_invalid_duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = customInput.toIntOrNull()
                        if (minutes != null && minutes > 0) {
                            onDurationSelected(minutes.minutes)
                            showCustomDialog = false
                        } else {
                            inputError = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_confirm), color = PrimaryFixedDim)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_cancel), color = OnSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun DurationChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KronoSpacing.sm)
    val background = if (isSelected) PrimaryMagenta else SurfaceContainerHigh
    val borderColor = if (isSelected) PrimaryMagenta else OutlineVariant
    val textColor = if (isSelected) OnSurface else OnSurfaceVariant

    Box(
        modifier = modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(shape)
            .background(background)
            .border(
                width = KronoSpacing.xs / 4,
                color = borderColor,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(KronoSpacing.xs / 2))
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
            }
        }
    }
}
