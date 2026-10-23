package com.krono.feature.tasks.edit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.krono.core.ui.brand.KronoSymbol
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.tasks.R

/**
 * Barra superior de navegación para la pantalla de edición de tarea (F-05).
 *
 * Basada en el sistema visual Lumina Glass y la referencia `/design/editar_tarea_krono`.
 *
 * @param onCancel Callback invocado al cancelar o pulsar volver.
 * @param onSave Callback opcional para guardar cambios directamente desde la barra superior.
 * @param canSave Indica si el botón de guardar está habilitado.
 * @param modifier Modificador de diseño.
 */
@Composable
fun EditTaskTopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Botón Cancelar (izquierda)
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(KronoSizes.minTouchTarget),
        ) {
            Icon(
                painter = painterResource(KronoIcons.ArrowBack),
                contentDescription = stringResource(R.string.cd_cancel_edit),
                tint = OnSurface,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }

        // Título central
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_task_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        }

        // Acción Guardar en la cabecera (derecha)
        TextButton(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.size(KronoSizes.minTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.action_confirm),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (canSave) PrimaryFixedDim else OnSurfaceVariant,
            )
        }
    }
}
