package com.krono.feature.tasks.focusmode

import androidx.annotation.StringRes
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.FocusPhase
import kotlin.time.Duration

/**
 * Estado de la pantalla del modo enfoque (F-09).
 *
 * @property task tarea en la que se trabaja. Se conserva aunque se elimine durante el enfoque, para
 * seguir mostrando su nombre hasta que se finalice.
 * @property isTaskDeleted la tarea se eliminó mientras corría el enfoque: la sesión se guardará
 * igualmente, pero su tiempo no se sumará a ninguna tarea.
 * @property phase fase del enfoque de esta tarea, o `null` si no hay un enfoque en curso para ella.
 * @property remaining tiempo que falta para terminar el bloque de trabajo.
 * @property blockDuration duración total del bloque de trabajo; con [remaining] da el progreso.
 * @property breakRemaining tiempo que falta para terminar el descanso, o `null` si no hay descanso.
 * @property breakDuration duración total del descanso en curso, o `null`; con [breakRemaining] da el progreso.
 * @property templateBreak descanso de la plantilla, que se ofrece al llegar a cero (CA-3).
 * @property isConfirmingSwitch hay otro cronómetro o enfoque activo y se pide confirmar que se detenga.
 * @property isBusy hay una acción en curso; los botones se deshabilitan para evitar dobles toques.
 * @property savedSession duración de la última sesión guardada, para confirmarla al usuario.
 * @property isSavedSessionWithoutTask la tarea ya no existía al finalizar: la sesión se guardó,
 * pero su tiempo no se sumó a ninguna tarea.
 * @property errorMessageRes mensaje de error genérico en español que hay que mostrar, si algo falló.
 */
data class FocusModeUiState(
    val isLoading: Boolean = true,
    val isTaskNotFound: Boolean = false,
    val task: Task? = null,
    val isTaskDeleted: Boolean = false,
    val phase: FocusPhase? = null,
    val remaining: Duration = Duration.ZERO,
    val blockDuration: Duration = Duration.ZERO,
    val breakRemaining: Duration? = null,
    val breakDuration: Duration? = null,
    val templateBreak: Duration = Duration.ZERO,
    val isConfirmingSwitch: Boolean = false,
    val isBusy: Boolean = false,
    val savedSession: Duration? = null,
    val isSavedSessionWithoutTask: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
) {
    private val orderedSteps get() = task?.subtasks.orEmpty().sortedBy { it.position }

    /** Cantidad de subtareas de la tarea; `0` si no tiene y no se muestra paso. */
    val totalSteps: Int get() = orderedSteps.size

    /** Número (desde 1) de la primera subtarea sin completar, o `null` si no hay o todas están hechas. */
    val currentStep: Int?
        get() = orderedSteps.indexOfFirst { !it.isDone }.takeIf { it >= 0 }?.plus(1)

    /** Título de la subtarea actual, o `null` si no hay paso actual. */
    val currentStepTitle: String? get() = currentStep?.let { orderedSteps[it - 1].title }

    /** Cantidad de subtareas ya completadas. */
    val completedSteps: Int get() = orderedSteps.count { it.isDone }

    /** La tarea tiene subtareas y todas están completas. */
    val areAllStepsDone: Boolean get() = totalSteps > 0 && currentStep == null

    /** Fracción del bloque de trabajo que falta, entre 0 y 1, para dibujar el anillo. */
    val remainingFraction: Float
        get() = if (blockDuration.isPositive()) (remaining / blockDuration).toFloat().coerceIn(0f, 1f) else 0f
}
