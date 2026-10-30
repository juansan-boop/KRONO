package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.JetBrainsMono
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focus.FocusTemplatesUiState
import com.krono.feature.tasks.focus.FocusTemplatesViewModel
import com.krono.feature.tasks.ui.TasksLoadErrorCard

/** Acciones de la pantalla de plantillas de enfoque, agrupadas para no repetir lambdas. */
data class FocusTemplatesActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onActivate: (String) -> Unit = {},
    val onRequestDelete: (String) -> Unit = {},
    val onDismissDelete: () -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
    val editor: FocusTemplateEditorActions = FocusTemplateEditorActions(),
)

/**
 * Punto de entrada con estado de la gestión de plantillas de enfoque (F-10). Conecta
 * [FocusTemplatesViewModel] con [FocusTemplatesScreen].
 *
 * @param onBack regresa a la pantalla anterior (Ajustes).
 */
@Composable
fun FocusTemplatesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusTemplatesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val actions = FocusTemplatesActions(
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onCreate = viewModel::onCreateClick,
        onEdit = viewModel::onEditClick,
        onActivate = viewModel::onActivate,
        onRequestDelete = viewModel::onRequestDelete,
        onDismissDelete = viewModel::onDismissDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        onMessageShown = viewModel::onMessageShown,
        editor = FocusTemplateEditorActions(
            onNameChange = viewModel::onNameChange,
            onFocusMinutesChange = viewModel::onFocusMinutesChange,
            onBreakMinutesChange = viewModel::onBreakMinutesChange,
            onSave = viewModel::onSave,
            onDismiss = viewModel::onDismissEditor,
        ),
    )
    FocusTemplatesScreen(state = state, actions = actions, modifier = modifier)
}

/**
 * Pantalla de plantillas de enfoque (F-10 CA-1 a CA-3): lista con la activa resaltada,
 * porcentaje de foco y descanso, y acciones de crear, editar, activar y eliminar, con estados de
 * carga, vacío y error.
 */
@Composable
fun FocusTemplatesScreen(
    state: FocusTemplatesUiState,
    actions: FocusTemplatesActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.messageRes?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            actions.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = DeepMidnight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!state.isLoading && !state.hasLoadError) CreateButton(actions.onCreate)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FocusTemplatesHeader(templateCount = state.templates.size, onBack = actions.onBack)
            when {
                state.isLoading -> LoadingContent()
                state.hasLoadError -> ErrorContent(actions.onRetry)
                state.templates.isEmpty() -> EmptyContent()
                else -> TemplatesList(state, actions)
            }
        }
    }

    state.editor?.let { FocusTemplateEditorDialog(state = it, actions = actions.editor) }
    state.templatePendingDelete?.let {
        ConfirmDeleteTemplateDialog(template = it, onDismiss = actions.onDismissDelete, onConfirm = actions.onConfirmDelete)
    }
}

@Composable
private fun FocusTemplatesHeader(templateCount: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(KronoSizes.minTouchTarget)) {
            Icon(
                painter = painterResource(KronoIcons.ArrowBack),
                contentDescription = stringResource(R.string.action_back),
                tint = OnSurface,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
            Text(
                text = stringResource(R.string.focus_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
            if (templateCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.focus_count, templateCount, templateCount),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = SecondaryCyan,
                )
            }
        }
    }
}

@Composable
private fun TemplatesList(state: FocusTemplatesUiState, actions: FocusTemplatesActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        item { IntroBanner() }
        items(state.templates, key = { it.id }) { template ->
            FocusTemplateCard(
                template = template,
                canDelete = state.canDelete,
                onActivate = { actions.onActivate(template.id) },
                onEdit = { actions.onEdit(template.id) },
                onDelete = { actions.onRequestDelete(template.id) },
            )
        }
    }
}

@Composable
private fun IntroBanner() {
    Text(
        text = stringResource(R.string.focus_intro),
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
    )
}

@Composable
private fun CreateButton(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(DeepMidnight).padding(KronoSpacing.md)) {
        Button(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
            shape = KronoShapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMagenta, contentColor = OnSurface),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
            Text(
                text = stringResource(R.string.focus_create),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = KronoSpacing.sm),
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    val description = stringResource(R.string.focus_cd_loading)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PrimaryFixedDim, strokeWidth = KronoSizes.progressStroke)
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(KronoSpacing.md)) {
        TasksLoadErrorCard(title = stringResource(R.string.focus_load_error_title), onRetry = onRetry)
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.focus_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.focus_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
