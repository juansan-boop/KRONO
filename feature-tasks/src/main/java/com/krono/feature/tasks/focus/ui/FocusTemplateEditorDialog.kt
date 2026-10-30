package com.krono.feature.tasks.focus.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.core.domain.focus.FocusTemplateLimits
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focus.FocusTemplateEditorState

/** Acciones del formulario de una plantilla de enfoque. */
data class FocusTemplateEditorActions(
    val onNameChange: (String) -> Unit = {},
    val onFocusMinutesChange: (String) -> Unit = {},
    val onBreakMinutesChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

/**
 * Formulario modal para crear o editar una plantilla de enfoque (F-10 CA-2). Valida nombre y
 * minutos; los límites que se muestran salen de [FocusTemplateLimits]. El contenido se desplaza
 * para seguir siendo usable con la fuente al 200 % y con el teclado abierto.
 */
@Composable
fun FocusTemplateEditorDialog(
    state: FocusTemplateEditorState,
    actions: FocusTemplateEditorActions,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = actions.onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
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
                EditorFields(state, actions)
                EditorButtons(state, actions)
            }
        }
    }
}

@Composable
private fun EditorFields(state: FocusTemplateEditorState, actions: FocusTemplateEditorActions) {
    Text(
        text = stringResource(
            if (state.isEditing) R.string.focus_editor_title_edit else R.string.focus_editor_title_create,
        ),
        style = MaterialTheme.typography.titleLarge,
        color = OnSurface,
    )
    EditorField(
        value = state.name,
        label = stringResource(R.string.focus_label_name),
        placeholder = stringResource(R.string.focus_placeholder_name),
        errorRes = state.nameErrorRes,
        keyboardType = KeyboardType.Text,
        onValueChange = actions.onNameChange,
    )
    EditorField(
        value = state.focusMinutesText,
        label = stringResource(R.string.focus_label_focus_minutes),
        supportingText = stringResource(
            R.string.focus_minutes_hint,
            FocusTemplateLimits.MIN_MINUTES,
            FocusTemplateLimits.MAX_FOCUS_MINUTES,
        ),
        errorRes = state.focusErrorRes,
        keyboardType = KeyboardType.Number,
        onValueChange = actions.onFocusMinutesChange,
    )
    EditorField(
        value = state.breakMinutesText,
        label = stringResource(R.string.focus_label_break_minutes),
        supportingText = stringResource(
            R.string.focus_minutes_hint,
            FocusTemplateLimits.MIN_MINUTES,
            FocusTemplateLimits.MAX_BREAK_MINUTES,
        ),
        errorRes = state.breakErrorRes,
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
        onValueChange = actions.onBreakMinutesChange,
    )
}

@Composable
private fun EditorButtons(state: FocusTemplateEditorState, actions: FocusTemplateEditorActions) {
    Button(
        onClick = actions.onSave,
        enabled = !state.isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
        shape = KronoShapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMagenta, contentColor = OnSurface),
    ) {
        Text(text = stringResource(R.string.focus_action_save), style = MaterialTheme.typography.labelLarge)
    }
    TextButton(
        onClick = actions.onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.minTouchTarget),
    ) {
        Text(
            text = stringResource(R.string.action_cancel),
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceVariant,
        )
    }
}

/** Campo de texto con la estética Lumina Glass, etiqueta, ayuda opcional y mensaje de error. */
@Composable
private fun EditorField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    @StringRes errorRes: Int?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = placeholder?.let { { Text(it, color = Outline) } },
        supportingText = {
            val message = errorRes?.let { stringResource(it) } ?: supportingText
            if (message != null) Text(message, color = if (errorRes != null) ErrorColor else OnSurfaceVariant)
        },
        isError = errorRes != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = KronoShapes.small,
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
            focusedLabelColor = PrimaryFixedDim,
            unfocusedLabelColor = OnSurfaceVariant,
        ),
    )
}

