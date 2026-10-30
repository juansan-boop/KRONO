package com.krono.feature.tasks.timer

import androidx.annotation.StringRes
import com.krono.core.domain.task.Task
import kotlin.time.Duration

/** Estado del cronómetro respecto a la tarea de la pantalla (F-08). */
enum class TimerStatus {
    /** No hay cronómetro en curso para esta tarea. */
    IDLE,

    /** El cronómetro de esta tarea está corriendo. */
    RUNNING,

    /** El cronómetro de esta tarea está en pausa. */
    PAUSED,
}

/**
 * Estado de la pantalla del cronómetro (F-08).
 *
 * @property task tarea que se mide, o `null` mientras carga o si no existe.
 * @property status estado del cronómetro de esta tarea.
 * @property elapsed tiempo trabajado en la sesión actual, sin pausas.
 * @property isConfirmingSwitch hay un cronómetro en otra tarea y se pide confirmar que se detenga (CA-4).
 * @property isBusy hay una acción en curso; los botones se deshabilitan para evitar dobles toques.
 * @property savedSession duración de la última sesión guardada, para confirmarla al usuario (CA-2).
 * @property isSavedSessionWithoutTask la tarea ya no existía al finalizar: la sesión se guardó,
 * pero su tiempo no se sumó a ninguna tarea y hay que avisarlo.
 * @property isSessionTooShort la sesión duró menos de [com.krono.core.domain.timer.FinishTimerUseCase.MIN_SESSION_DURATION]:
 * se descartó, no se guardó ni se sumó a la tarea, y hay que avisarlo.
 * @property errorMessageRes recurso del mensaje de error que hay que mostrar, si algo falló. Es un
 * texto genérico en español: los detalles técnicos del error no llegan a la pantalla.
 */
data class TaskTimerUiState(
    val isLoading: Boolean = true,
    val isTaskNotFound: Boolean = false,
    val task: Task? = null,
    val status: TimerStatus = TimerStatus.IDLE,
    val elapsed: Duration = Duration.ZERO,
    val isConfirmingSwitch: Boolean = false,
    val isBusy: Boolean = false,
    val savedSession: Duration? = null,
    val isSavedSessionWithoutTask: Boolean = false,
    val isSessionTooShort: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
) {
    /** Fracción de la estimación ya trabajada, entre 0 y 1, o `null` si la tarea no tiene estimación. */
    val estimateProgress: Float?
        get() {
            val estimate = task?.estimatedDuration?.takeIf { it.isPositive() } ?: return null
            return (elapsed / estimate).toFloat().coerceIn(0f, 1f)
        }
}
