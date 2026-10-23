package com.krono.feature.tasks.edit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.create.ui.CategorySelector
import com.krono.feature.tasks.create.ui.DueDateTimePicker
import com.krono.feature.tasks.create.ui.DurationSelector
import com.krono.feature.tasks.create.ui.PrioritySelector
import com.krono.feature.tasks.create.ui.TaskTitleField
import com.krono.feature.tasks.edit.EditTaskEvent
import com.krono.feature.tasks.edit.EditTaskUiState
import com.krono.feature.tasks.edit.EditTaskViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/**
 * Punto de entrada con estado para la pantalla de edición de tarea (F-05).
 * Conecta [EditTaskViewModel] con los eventos unidireccionales y la vista.
 *
 * @param onBack Callback invocado tras guardar o cancelar la edición.
 * @param modifier Modificador de diseño.
 * @param viewModel Instancia inyectada de [EditTaskViewModel].
 */
@Composable
fun EditTaskRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditTaskEvent.NavigateBack -> onBack()
                is EditTaskEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    EditTaskScreen(
        state = state,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onPrioritySelected = viewModel::onPrioritySelected,
        onDurationSelected = viewModel::onDurationSelected,
        onSaveChanges = viewModel::saveChanges,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Pantalla principal de edición de tarea (F-05 CA-1 a CA-3).
 * Implementa el formulario precargado bajo el Lumina Glass System sobre fondo Deep Midnight.
 *
 * @param state Estado reactivo inmutable.
 * @param onBack Callback para volver a la pantalla anterior.
 * @param onTitleChanged Modificación del título de la tarea.
 * @param onCategorySelected Selección de categoría.
 * @param onDateSelected Selección de fecha límite.
 * @param onTimeSelected Selección de hora límite.
 * @param onPrioritySelected Selección de prioridad.
 * @param onDurationSelected Selección de duración estimada opcional.
 * @param onSaveChanges Intento de guardado de los cambios realizados.
 * @param modifier Modificador de diseño.
 * @param snackbarHostState Estado de mensajes temporales.
 */
@Composable
fun EditTaskScreen(
    state: EditTaskUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategorySelected: (TaskCategory) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onDurationSelected: (Duration?) -> Unit,
    onSaveChanges: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepMidnight,
        topBar = {
            EditTaskTopBar(
                onCancel = onBack,
                onSave = onSaveChanges,
                canSave = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.isLoading -> {
                    // Estado de carga inicial (CA-1)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(KronoSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryMagenta,
                            strokeWidth = KronoSizes.progressStroke,
                        )
                        Spacer(modifier = Modifier.height(KronoSpacing.md))
                        Text(
                            text = stringResource(R.string.loading_task),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                state.isTaskNotFound -> {
                    // Estado cuando la tarea no existe
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = KronoSizes.formMaxWidth)
                            .padding(KronoSpacing.lg)
                            .glassSurface()
                            .padding(KronoSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                    ) {
                        Icon(
                            painter = painterResource(KronoIcons.Error),
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(KronoSizes.iconLarge),
                        )
                        Text(
                            text = stringResource(R.string.error_task_not_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedButton(
                            onClick = onBack,
                            shape = KronoShapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.action_back),
                                color = OnSurface,
                            )
                        }
                    }
                }

                else -> {
                    // Formulario de edición completo (CA-1, CA-2, CA-3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = KronoSizes.formMaxWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                    ) {
                        // Tarjeta Lumina Glass
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface()
                                .padding(KronoSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                        ) {
                            // Banner pill superior: referencia /design/editar_tarea_krono
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(KronoShapes.extraSmall)
                                    .background(PrimaryMagenta.copy(alpha = BANNER_BG_ALPHA))
                                    .border(
                                        width = KronoSpacing.xs / 4,
                                        color = PrimaryMagenta.copy(alpha = BANNER_BORDER_ALPHA),
                                        shape = KronoShapes.extraSmall,
                                    )
                                    .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_note),
                                    contentDescription = null,
                                    tint = PrimaryFixedDim,
                                    modifier = Modifier.size(KronoSizes.iconSmall),
                                )
                                Spacer(modifier = Modifier.width(KronoSpacing.xs))
                                Text(
                                    text = stringResource(R.string.edit_task_banner),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryFixedDim,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            // Título de la sección
                            Column {
                                Text(
                                    text = stringResource(R.string.edit_task_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(KronoSpacing.xs))
                                Text(
                                    text = stringResource(R.string.edit_task_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                )
                            }

                            // Campo 1: Nombre de la tarea (CA-1, CA-3)
                            TaskTitleField(
                                title = state.title,
                                onTitleChanged = onTitleChanged,
                                isError = state.isTitleError,
                                errorRes = state.titleErrorRes,
                            )

                            // Campo 2: Selector de Categoría (CA-1)
                            CategorySelector(
                                selectedCategory = state.category,
                                onCategorySelected = onCategorySelected,
                            )

                            // Campo 3: Selector de Fecha y Hora (CA-1, CA-3)
                            DueDateTimePicker(
                                dueDate = state.dueDate,
                                dueTime = state.dueTime,
                                isDateError = state.isDateError,
                                isTimeError = state.isTimeError,
                                errorRes = state.dateErrorRes ?: state.dateTimeErrorRes,
                                onDateSelected = onDateSelected,
                                onTimeSelected = onTimeSelected,
                            )

                            // Campo 4: Selector de Prioridad (CA-1)
                            PrioritySelector(
                                selectedPriority = state.priority,
                                onPrioritySelected = onPrioritySelected,
                            )

                            // Campo 5: Estimación de duración (CA-1)
                            DurationSelector(
                                selectedDuration = state.estimatedDuration,
                                onDurationSelected = onDurationSelected,
                            )

                            Spacer(modifier = Modifier.height(KronoSpacing.xs))

                            // Botón Principal Inferior "Guardar cambios" (CA-2)
                            Button(
                                onClick = onSaveChanges,
                                enabled = state.canSubmit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = KronoSizes.controlHeight),
                                shape = KronoShapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryMagenta,
                                    contentColor = OnSurface,
                                    disabledContainerColor = SurfaceContainerHigh,
                                    disabledContentColor = OnSurfaceVariant,
                                ),
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(KronoSizes.iconSmall),
                                        color = OnSurface,
                                        strokeWidth = KronoSizes.progressStroke,
                                    )
                                    Spacer(modifier = Modifier.width(KronoSpacing.sm))
                                    Text(
                                        text = stringResource(R.string.action_saving_changes),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.action_save_changes),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        // Espacio inferior para scroll cómodo
                        Spacer(modifier = Modifier.height(KronoSpacing.lg))
                    }
                }
            }
        }
    }
}

private const val BANNER_BG_ALPHA = 0.12f
private const val BANNER_BORDER_ALPHA = 0.3f
