package com.krono.feature.tasks.create.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.create.CreateTaskEvent
import com.krono.feature.tasks.create.CreateTaskUiState
import com.krono.feature.tasks.create.CreateTaskViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/**
 * Punto de entrada con estado para la pantalla de creación de tarea (F-04).
 * Conecta [CreateTaskViewModel] con los eventos y la interfaz visual.
 *
 * @param onBack Callback invocado tras persistir la tarea o al pulsar el botón de volver.
 * @param modifier Modificador de diseño.
 * @param viewModel Instancia inyectada de [CreateTaskViewModel].
 */
@Composable
fun CreateTaskRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateTaskEvent.NavigateBack -> onBack()
                is CreateTaskEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    CreateTaskScreen(
        state = state,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onPrioritySelected = viewModel::onPrioritySelected,
        onDurationSelected = viewModel::onDurationSelected,
        onApplyDurationSuggestion = viewModel::onApplyDurationSuggestion,
        onSaveTask = viewModel::saveTask,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Pantalla principal de creación de tarea (F-04 CA-1 a CA-4).
 * Implementa el formulario completo con estética Lumina Glass sobre fondo Deep Midnight.
 *
 * @param state Estado reactivo inmutable.
 * @param onBack Callback para volver a la pantalla anterior.
 * @param onTitleChanged Modificación del nombre de la tarea.
 * @param onCategorySelected Selección de categoría.
 * @param onDateSelected Selección de fecha límite.
 * @param onTimeSelected Selección de hora límite.
 * @param onPrioritySelected Selección de prioridad.
 * @param onDurationSelected Selección de duración estimada opcional.
 * @param onApplyDurationSuggestion Aplica la duración sugerida por historial (F-11).
 * @param onSaveTask Intento de guardado del formulario.
 * @param modifier Modificador de diseño.
 * @param snackbarHostState Estado para mensajes temporales.
 */
@Composable
fun CreateTaskScreen(
    state: CreateTaskUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategorySelected: (TaskCategory) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onDurationSelected: (Duration?) -> Unit,
    onSaveTask: () -> Unit,
    onApplyDurationSuggestion: () -> Unit = {},
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepMidnight,
        topBar = {
            CreateTaskTopBar(
                onBack = onBack,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = KronoSizes.formMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                // Tarjeta modal Lumina Glass
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface()
                        .padding(KronoSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                ) {
                    // Encabezado
                    Column {
                        Text(
                            text = stringResource(R.string.create_task_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(KronoSpacing.xs))
                        Text(
                            text = stringResource(R.string.create_task_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }

                    // Campo 1: Nombre de la tarea (CA-1, CA-2)
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

                    // Campo 3: Selector de Fecha y Hora (CA-1, CA-2, CA-3)
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

                    // Campo 5: Estimación rápida / duración opcional (CA-1, CA-4)
                    DurationSelector(
                        selectedDuration = state.estimatedDuration,
                        onDurationSelected = onDurationSelected,
                    )

                    // Sugerencia por historial (F-11 CA-1, CA-2)
                    if (state.showDurationSuggestion) {
                        state.durationSuggestion?.let { suggestion ->
                            DurationSuggestionChip(
                                suggestion = suggestion,
                                onApply = onApplyDurationSuggestion,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(KronoSpacing.xs))

                    // Botón Principal Inferior "Guardar tarea" (CA-1)
                    Button(
                        onClick = onSaveTask,
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
                                text = stringResource(R.string.action_saving_task),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_save_task),
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
