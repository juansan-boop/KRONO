package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Confirmación para eliminar una plantilla de enfoque (F-10 CA-2). Si la plantilla es la activa
 * avisa de que se activará otra. La acción segura, conservar, es la principal.
 *
 * @param template plantilla que se quiere eliminar.
 * @param onDismiss conserva la plantilla y cierra el diálogo.
 * @param onConfirm elimina la plantilla.
 */
@Composable
fun ConfirmDeleteTemplateDialog(
    template: FocusTemplate,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = KronoSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = KronoSizes.formMaxWidth)
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = KronoGlass.cornerRadius)
                    .verticalScroll(rememberScrollState())
                    .padding(KronoSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                Text(
                    text = stringResource(R.string.focus_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                )
                Text(
                    text = stringResource(R.string.focus_delete_body, template.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                if (template.isActive) {
                    Text(
                        text = stringResource(R.string.focus_delete_active_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryCyan,
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = KronoShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMagenta, contentColor = OnSurface),
                ) {
                    Text(text = stringResource(R.string.focus_delete_keep), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = KronoShapes.small,
                    border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = DESTRUCTIVE_BORDER_ALPHA)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ErrorContainer.copy(alpha = DESTRUCTIVE_CONTAINER_ALPHA),
                        contentColor = ErrorColor,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.focus_delete_confirm),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private const val DESTRUCTIVE_CONTAINER_ALPHA = 0.2f
private const val DESTRUCTIVE_BORDER_ALPHA = 0.5f
