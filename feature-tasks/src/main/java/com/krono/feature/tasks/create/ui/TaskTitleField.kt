package com.krono.feature.tasks.create.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R

/**
 * Campo de texto para ingresar el nombre de la tarea con validación y estilo Lumina Glass (F-04 CA-1, CA-2).
 *
 * @param title Texto actual del título.
 * @param onTitleChanged Callback invocado cuando cambia el texto.
 * @param isError Indica si el campo tiene un error de validación.
 * @param errorRes Recurso de texto de error a desplegar si [isError] es verdadero.
 * @param modifier Modificador de diseño.
 */
@Composable
fun TaskTitleField(
    title: String,
    onTitleChanged: (String) -> Unit,
    isError: Boolean,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Etiqueta superior
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_note),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(R.string.label_task_name),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(KronoSpacing.xs))

        // Campo de entrada
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.placeholder_task_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Outline,
                )
            },
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(KronoSpacing.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                errorTextColor = OnSurface,
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                errorContainerColor = SurfaceContainerLow,
                cursorColor = PrimaryFixedDim,
                errorCursorColor = ErrorColor,
                focusedBorderColor = PrimaryFixedDim,
                unfocusedBorderColor = OutlineVariant,
                errorBorderColor = ErrorColor,
            ),
        )

        // Mensaje descriptivo de error (CA-2)
        if (isError && errorRes != null) {
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
}
