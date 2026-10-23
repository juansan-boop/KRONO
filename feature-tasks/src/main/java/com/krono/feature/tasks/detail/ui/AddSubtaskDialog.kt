package com.krono.feature.tasks.detail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R

/**
 * Diálogo accesible para añadir una nueva subtarea a la tarea (F-06 CA-2).
 *
 * @param onConfirm Callback invocado con el título de la nueva subtarea.
 * @param onDismiss Callback invocado al cancelar el diálogo.
 */
@Composable
fun AddSubtaskDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var stepTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.task_detail_add_step_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stepTitle,
                    onValueChange = { stepTitle = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.task_detail_add_step_hint),
                            color = Outline,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    singleLine = true,
                    shape = KronoShapes.extraSmall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow,
                        focusedBorderColor = PrimaryFixedDim,
                        unfocusedBorderColor = OutlineVariant,
                        cursorColor = PrimaryFixedDim,
                    ),
                )
                Spacer(modifier = Modifier.height(KronoSpacing.xs))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (stepTitle.isNotBlank()) {
                        onConfirm(stepTitle.trim())
                    }
                },
                enabled = stepTitle.isNotBlank(),
            ) {
                Text(
                    text = stringResource(R.string.action_confirm),
                    color = if (stepTitle.isNotBlank()) PrimaryFixedDim else OnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = OnSurfaceVariant,
                )
            }
        },
        containerColor = SurfaceContainerHigh,
        shape = KronoShapes.medium,
    )
}
