package com.krono.feature.tasks.detail.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
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
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.detail.RemainingTime
import com.krono.feature.tasks.detail.TaskDetailEvent
import com.krono.feature.tasks.detail.TaskDetailUiState
import com.krono.feature.tasks.detail.TaskDetailViewModel
import com.krono.feature.tasks.ui.ConfirmDeleteTaskDialog
import com.krono.feature.tasks.ui.TaskDateFormatter
import com.krono.feature.tasks.ui.TasksLoadErrorCard
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Punto de entrada con estado para la pantalla de detalle de tarea y subtareas (F-06, F-07).
 * Conecta [TaskDetailViewModel] con los eventos y la navegación.
 *
 * @param onBack Callback para volver a la pantalla anterior.
 * @param onEditTask Callback para navegar al formulario de edición (F-05).
 * @param onStartFocus Callback para iniciar el modo enfoque con esta tarea (F-09).
 * @param modifier Modificador de diseño.
 * @param onStartTimer Callback para abrir el cronómetro de esta tarea (F-08); sin él no se muestra el botón.
 * @param viewModel Instancia inyectada de [TaskDetailViewModel].
 */
@Composable
fun TaskDetailRoute(
    onBack: () -> Unit,
    onEditTask: (String) -> Unit,
    onStartFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartTimer: ((String) -> Unit)? = null,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskDetailEvent.NavigateBack -> onBack()
                is TaskDetailEvent.NavigateToEdit -> onEditTask(event.taskId)
                is TaskDetailEvent.NavigateToFocus -> onStartFocus(event.taskId)
                is TaskDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    TaskDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = viewModel::onEditTaskClicked,
        onDelete = viewModel::onDeleteTaskClicked,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onConfirmDelete = viewModel::onConfirmDeleteTask,
        onPostpone = viewModel::onPostponeTaskToTomorrow,
        onToggleSubtask = viewModel::onToggleSubtask,
        onOpenAddStepDialog = viewModel::onOpenAddSubtaskDialog,
        onDismissAddStepDialog = viewModel::onDismissAddSubtaskDialog,
        onConfirmAddStep = viewModel::onAddSubtask,
        onStartFocus = viewModel::onStartFocusClicked,
        onStartTimer = onStartTimer?.let { open -> fun() { state.task?.id?.let(open) } },
        onRetryLoad = viewModel::onRetryLoad,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Pantalla principal del detalle de tarea y subtareas (F-06 CA-1, CA-2, CA-3, F-07 CA-1, CA-2, CA-3).
 *
 * @param state Estado reactivo inmutable.
 * @param onBack Callback para volver a la lista de tareas.
 * @param onEdit Callback para solicitar edición.
 * @param onToggleSubtask Callback para conmutar un paso de la subtarea.
 * @param onOpenAddStepDialog Abre el diálogo de añadir paso.
 * @param onDismissAddStepDialog Cierra el diálogo de añadir paso.
 * @param onConfirmAddStep Confirma la creación de una subtarea.
 * @param onStartFocus Solicita inicio de modo enfoque.
 * @param modifier Modificador de diseño.
 * @param onDelete Callback para abrir el diálogo de confirmación de eliminación (F-07).
 * @param onDismissDeleteDialog Callback para descartar el diálogo de eliminación (F-07).
 * @param onConfirmDelete Callback para confirmar la eliminación de la tarea (F-07).
 * @param onPostpone Callback para posponer la tarea para mañana (F-07).
 * @param onStartTimer Abre el cronómetro de la tarea (F-08); sin él no se muestra el botón.
 * @param onRetryLoad Reabre la lectura de la tarea cuando falló.
 * @param snackbarHostState Estado para mensajes temporales.
 */
@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onOpenAddStepDialog: () -> Unit,
    onDismissAddStepDialog: () -> Unit,
    onConfirmAddStep: (String) -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onDismissDeleteDialog: (() -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    onPostpone: (() -> Unit)? = null,
    onStartTimer: (() -> Unit)? = null,
    onRetryLoad: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepMidnight,
        topBar = {
            TaskDetailTopBar(
                onBack = onBack,
                onEdit = onEdit,
                onDelete = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.task != null) {
                // Barra flotante inferior: Iniciar Enfoque (CA-3)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    val focusButtonText = if (state.task.estimatedDuration != null) {
                        val durationText = TaskDateFormatter.formatEstimatedDuration(context, state.task.estimatedDuration)
                            ?: ""
                        stringResource(R.string.task_detail_focus_button, durationText)
                    } else {
                        stringResource(R.string.task_detail_focus_button_no_time)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = KronoSizes.formMaxWidth),
                        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                    ) {
                        // Cronómetro de duración real (F-08)
                        onStartTimer?.let { TimerButton(onClick = it) }

                        Button(
                            onClick = onStartFocus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = KronoSizes.controlHeight),
                            shape = KronoShapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryMagenta,
                                contentColor = OnSurface,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_auto_awesome),
                                contentDescription = null,
                                tint = OnSurface,
                                modifier = Modifier.size(KronoSizes.iconMedium),
                            )
                            Spacer(modifier = Modifier.width(KronoSpacing.sm))
                            Text(
                                text = focusButtonText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.isLoading -> {
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

                state.hasLoadError -> {
                    TasksLoadErrorCard(
                        title = stringResource(R.string.task_load_error_title),
                        onRetry = onRetryLoad,
                        modifier = Modifier
                            .widthIn(max = KronoSizes.formMaxWidth)
                            .padding(KronoSpacing.lg),
                    )
                }

                state.isTaskNotFound || state.task == null -> {
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
                    val currentTask = state.task
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = KronoSizes.formMaxWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                    ) {
                        // Hero Header: Categoría, Prioridad y Título de la tarea
                        HeroHeader(task = currentTask)

                        // Tarjeta de información clave (Lumina Glass) (CA-1)
                        KeyInfoCard(
                            task = currentTask,
                            remainingTime = state.remainingTime,
                        )

                        // Sección de subtareas (CA-1, CA-2)
                        SubtaskSection(
                            subtasks = currentTask.subtasks,
                            completedCount = state.completedSubtasksCount,
                            totalCount = state.totalSubtasksCount,
                            progressRatio = state.progressRatio,
                            onToggleSubtask = onToggleSubtask,
                            onOpenAddStepDialog = onOpenAddStepDialog,
                        )

                        // Tarjeta de Nota rápida si existe (CA-1)
                        val note = currentTask.note
                        if (!note.isNullOrBlank()) {
                            QuickNoteCard(note = note)
                        }

                        // Espaciado inferior para scroll cómodo
                        Spacer(modifier = Modifier.height(KronoSpacing.lg))
                    }
                }
            }

            // Diálogo para añadir paso
            if (state.isAddingSubtask) {
                AddSubtaskDialog(
                    onConfirm = onConfirmAddStep,
                    onDismiss = onDismissAddStepDialog,
                )
            }

            // Diálogo de confirmación para eliminar o posponer tarea (F-07)
            if (state.isConfirmingDelete && state.task != null && onDismissDeleteDialog != null && onConfirmDelete != null && onPostpone != null) {
                ConfirmDeleteTaskDialog(
                    task = state.task,
                    today = today,
                    onDismiss = onDismissDeleteDialog,
                    onConfirmDelete = onConfirmDelete,
                    onPostpone = onPostpone,
                )
            }
        }
    }
}

/**
 * Encabezado principal que muestra categoría, prioridad y título grande.
 */
@Composable
private fun HeroHeader(
    task: Task,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge de Categoría
            DetailCategoryBadge(category = task.category)

            // Badge de Prioridad
            DetailPriorityBadge(priority = task.priority)
        }

        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
    }
}

/**
 * Tarjeta de información clave con límite de entrega, tiempo restante y estimación (CA-1).
 */
@Composable
private fun KeyInfoCard(
    task: Task,
    remainingTime: RemainingTime?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        // Fila 1: Límite de entrega y tiempo restante (CA-1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.CalendarToday),
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Column {
                    Text(
                        text = stringResource(R.string.task_detail_due_date_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.task_detail_due_date_format,
                            TaskDateFormatter.formatDueDate(context, task.dueDate, today),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                }
            }

            // Insignia de tiempo restante ("En 3 horas")
            if (remainingTime != null) {
                val formattedRemaining = TaskDateFormatter.formatRemainingTime(context, remainingTime)
                Text(
                    text = formattedRemaining,
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryCyan,
                    modifier = Modifier
                        .clip(KronoShapes.extraSmall)
                        .background(SecondaryCyan.copy(alpha = BADGE_ALPHA))
                        .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
                )
            }
        }

        HorizontalDivider(
            color = OutlineVariant.copy(alpha = DIVIDER_ALPHA),
            thickness = KronoGlass.borderWidth,
        )

        // Fila 2: Estimación KRONO (CA-1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Column {
                    Text(
                        text = stringResource(R.string.task_detail_estimate_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                    val estText = task.estimatedDuration?.let {
                        TaskDateFormatter.formatEstimatedDuration(context, it)
                    } ?: stringResource(R.string.no_due_date)
                    Text(
                        text = stringResource(R.string.task_detail_estimate_value, estText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                }
            }

            // Insignia zen de estimación
            Text(
                text = stringResource(R.string.task_detail_estimate_badge),
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryFixedDim,
                modifier = Modifier
                    .clip(KronoShapes.extraSmall)
                    .background(PrimaryMagenta.copy(alpha = BADGE_ALPHA))
                    .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
            )
        }
    }
}

/**
 * Tarjeta para visualizar la nota rápida de la tarea (CA-1).
 */
@Composable
private fun QuickNoteCard(
    note: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit_note),
            contentDescription = null,
            tint = SecondaryCyan,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
            Text(
                text = stringResource(R.string.task_detail_quick_note_label),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
            )
        }
    }
}

@Composable
private fun DetailCategoryBadge(category: TaskCategory) {
    val (iconRes, textRes, tintColor) = when (category) {
        TaskCategory.ESTUDIO -> Triple(R.drawable.ic_school, R.string.filter_study, PrimaryFixedDim)
        TaskCategory.TRABAJO -> Triple(R.drawable.ic_work, R.string.filter_work, PrimaryFixedDim)
        TaskCategory.PERSONAL -> Triple(R.drawable.ic_person, R.string.filter_personal, SecondaryCyan)
    }
    val badgeShape = RoundedCornerShape(KronoSpacing.sm)

    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(tintColor.copy(alpha = BADGE_ALPHA))
            .border(width = KronoGlass.borderWidth, color = tintColor.copy(alpha = BADGE_BORDER_ALPHA), shape = badgeShape)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
        )
    }
}

@Composable
private fun DetailPriorityBadge(priority: TaskPriority) {
    val (textRes, tintColor) = when (priority) {
        TaskPriority.ALTA -> Pair(R.string.priority_high, PrimaryFixedDim)
        TaskPriority.MEDIA -> Pair(R.string.priority_medium, PrimaryFixedDim)
        TaskPriority.BAJA -> Pair(R.string.priority_low, OnSurfaceVariant)
    }
    val badgeShape = RoundedCornerShape(KronoSpacing.sm)

    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(tintColor.copy(alpha = BADGE_ALPHA))
            .border(width = KronoGlass.borderWidth, color = tintColor.copy(alpha = BADGE_BORDER_ALPHA), shape = badgeShape)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
        )
    }
}

private const val BADGE_ALPHA = 0.12f
private const val BADGE_BORDER_ALPHA = 0.35f
private const val DIVIDER_ALPHA = 0.4f
