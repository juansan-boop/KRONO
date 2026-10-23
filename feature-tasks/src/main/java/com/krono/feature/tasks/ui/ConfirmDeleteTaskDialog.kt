package com.krono.feature.tasks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.core.domain.task.Task
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import kotlinx.datetime.LocalDate

/**
 * Diálogo modal para confirmar la eliminación o posposición de una tarea (F-07 CA-1, CA-2, CA-3).
 *
 * Implementa la estética Lumina Glass System con fondo translúcido, halo sutil de advertencia
 * y resumen de la tarea incluyendo el conteo de subtareas que se descartarán (CA-1).
 *
 * @param task Tarea que se solicita eliminar.
 * @param today Fecha de referencia para formatear la fecha límite.
 * @param onDismiss Callback invocado para cancelar y conservar la tarea (CA-2).
 * @param onConfirmDelete Callback invocado para confirmar la eliminación de la tarea y sus subtareas (CA-2).
 * @param onPostpone Callback invocado para posponer la tarea para mañana sin eliminarla (CA-3).
 * @param modifier Modificador de diseño.
 */
@Composable
fun ConfirmDeleteTaskDialog(
    task: Task,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    onPostpone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dialogDescription = stringResource(R.string.cd_confirm_delete_dialog)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = KronoSpacing.lg)
                .semantics {
                    this.contentDescription = dialogDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = KronoSizes.formMaxWidth)
                    .glassSurface(cornerRadius = KronoGlass.cornerRadius)
                    .padding(KronoSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Icono de advertencia / eliminación con contenedor redondeado
                Box(
                    modifier = Modifier
                        .size(KronoSizes.controlHeight)
                        .clip(KronoShapes.small)
                        .background(ErrorContainer.copy(alpha = ICON_CONTAINER_BG_ALPHA))
                        .border(
                            width = KronoGlass.borderWidth,
                            color = ErrorColor.copy(alpha = ICON_CONTAINER_BORDER_ALPHA),
                            shape = KronoShapes.small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(KronoSizes.iconLarge),
                    )
                }

                Spacer(modifier = Modifier.height(KronoSpacing.md))

                // Título y descripción empática
                Text(
                    text = stringResource(R.string.delete_task_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(KronoSpacing.xs))

                Text(
                    text = stringResource(R.string.delete_task_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(KronoSpacing.md))

                // Tarjeta resumen de la tarea a eliminar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(KronoShapes.small)
                        .background(SurfaceContainerLow)
                        .border(
                            width = KronoGlass.borderWidth,
                            color = OutlineVariant.copy(alpha = CARD_BORDER_ALPHA),
                            shape = KronoShapes.small,
                        )
                        .padding(KronoSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                ) {
                    // Estado y fecha límite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val statusText = if (task.isCompleted) {
                            stringResource(R.string.action_mark_completed)
                        } else {
                            stringResource(R.string.delete_task_status_in_progress)
                        }

                        Row(
                            modifier = Modifier
                                .clip(KronoShapes.extraSmall)
                                .background(SecondaryCyan.copy(alpha = BADGE_BG_ALPHA))
                                .border(
                                    width = KronoGlass.borderWidth,
                                    color = SecondaryCyan.copy(alpha = BADGE_BORDER_ALPHA),
                                    shape = KronoShapes.extraSmall,
                                )
                                .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(KronoSpacing.xs + KronoSpacing.xs / 2)
                                    .clip(CircleShape)
                                    .background(SecondaryCyan),
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryCyan,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_schedule),
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(KronoSizes.iconSmall),
                            )
                            Text(
                                text = TaskDateFormatter.formatDueDate(context, task.dueDate, today),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(KronoSpacing.xs / 2))

                    // Título de la tarea
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(KronoSpacing.xs / 2))

                    // Conteo de subtareas que se descartarán (CA-1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                    ) {
                        Icon(
                            painter = painterResource(KronoIcons.TaskAlt),
                            contentDescription = null,
                            tint = PrimaryFixedDim,
                            modifier = Modifier.size(KronoSizes.iconSmall),
                        )
                        val subtasksCount = task.subtasks.size
                        val subtasksMessage = when (subtasksCount) {
                            0 -> stringResource(R.string.delete_task_subtasks_none)
                            1 -> stringResource(R.string.delete_task_subtasks_single)
                            else -> stringResource(R.string.delete_task_subtasks_multiple, subtasksCount)
                        }
                        Text(
                            text = subtasksMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(KronoSpacing.md))

                // Botón primario y seguro: Conservar tarea (CA-2)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoSizes.controlHeight),
                    shape = KronoShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryMagenta,
                        contentColor = OnSurface,
                    ),
                ) {
                    Icon(
                        painter = painterResource(KronoIcons.CheckCircle),
                        contentDescription = null,
                        tint = OnSurface,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                    Spacer(modifier = Modifier.width(KronoSpacing.sm))
                    Text(
                        text = stringResource(R.string.delete_task_keep_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(KronoSpacing.sm))

                // Botón destructivo con alerta: Sí, eliminar tarea (CA-2)
                OutlinedButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoSizes.controlHeight),
                    shape = KronoShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ErrorContainer.copy(alpha = DESTRUCTIVE_CONTAINER_ALPHA),
                        contentColor = ErrorColor,
                    ),
                    border = BorderStroke(
                        width = KronoGlass.borderWidth,
                        color = ErrorColor.copy(alpha = DESTRUCTIVE_BORDER_ALPHA),
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                    Spacer(modifier = Modifier.width(KronoSpacing.sm))
                    Text(
                        text = stringResource(R.string.delete_task_confirm_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorColor,
                    )
                }

                Spacer(modifier = Modifier.height(KronoSpacing.xs))

                // Botón alternativo: Posponer para mañana sin borrar (CA-3)
                TextButton(
                    onClick = onPostpone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(modifier = Modifier.width(KronoSpacing.xs))
                    Text(
                        text = stringResource(R.string.delete_task_postpone_action),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryCyan,
                    )
                }
            }
        }
    }
}

private const val ICON_CONTAINER_BG_ALPHA = 0.25f
private const val ICON_CONTAINER_BORDER_ALPHA = 0.4f
private const val CARD_BORDER_ALPHA = 0.4f
private const val BADGE_BG_ALPHA = 0.15f
private const val BADGE_BORDER_ALPHA = 0.35f
private const val DESTRUCTIVE_CONTAINER_ALPHA = 0.2f
private const val DESTRUCTIVE_BORDER_ALPHA = 0.5f
