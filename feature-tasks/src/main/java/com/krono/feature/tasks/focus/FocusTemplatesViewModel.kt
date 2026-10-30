package com.krono.feature.tasks.focus

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.focus.ActivateFocusTemplateUseCase
import com.krono.core.domain.focus.CreateFocusTemplateUseCase
import com.krono.core.domain.focus.DeleteFocusTemplateUseCase
import com.krono.core.domain.focus.EditFocusTemplateUseCase
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.FocusTemplateError
import com.krono.core.domain.focus.FocusTemplateLimits
import com.krono.core.domain.focus.FocusTemplateValidator
import com.krono.core.domain.focus.ObserveFocusTemplatesUseCase
import com.krono.feature.tasks.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de plantillas de enfoque (F-10): lista con la activa primero, formulario
 * de crear y editar, eliminación con confirmación y cambio de plantilla activa.
 *
 * Regla dura de arquitectura: nunca importa `androidx.compose.*`; expone su estado con [StateFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusTemplatesViewModel @Inject constructor(
    observeTemplates: ObserveFocusTemplatesUseCase,
    private val createTemplate: CreateFocusTemplateUseCase,
    private val editTemplate: EditFocusTemplateUseCase,
    private val deleteTemplate: DeleteFocusTemplateUseCase,
    private val activateTemplate: ActivateFocusTemplateUseCase,
) : ViewModel() {

    private val editor = MutableStateFlow<FocusTemplateEditorState?>(null)
    private val pendingDeleteId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<Int?>(null)

    /** Cada incremento vuelve a abrir la observación tras un fallo de lectura. */
    private val loadAttempts = MutableStateFlow(0)

    private val templatesResult: Flow<KronoResult<List<FocusTemplate>>> = loadAttempts.flatMapLatest {
        observeTemplates().onStart { emit(KronoResult.Loading) }
    }

    /** Estado reactivo observable por la interfaz. */
    val uiState: StateFlow<FocusTemplatesUiState> = combine(
        templatesResult,
        editor,
        pendingDeleteId,
        message,
    ) { result, editorState, deleteId, messageRes ->
        when (result) {
            KronoResult.Loading -> FocusTemplatesUiState(isLoading = true)
            is KronoResult.Error -> FocusTemplatesUiState(isLoading = false, hasLoadError = true, messageRes = messageRes)
            is KronoResult.Success -> FocusTemplatesUiState(
                isLoading = false,
                templates = result.data,
                editor = editorState,
                templatePendingDelete = result.data.firstOrNull { it.id == deleteId },
                messageRes = messageRes,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FocusTemplatesUiState(),
    )

    /** Vuelve a abrir la observación después de un error de lectura. */
    fun onRetry() {
        loadAttempts.update { it + 1 }
    }

    /** Abre el formulario para crear una plantilla con minutos iniciales razonables. */
    fun onCreateClick() {
        editor.value = FocusTemplateEditorState(
            focusMinutesText = DEFAULT_FOCUS_MINUTES.toString(),
            breakMinutesText = DEFAULT_BREAK_MINUTES.toString(),
        )
    }

    /** Abre el formulario con los datos de la plantilla [id]; no hace nada si ya no existe. */
    fun onEditClick(id: String) {
        val template = uiState.value.templates.firstOrNull { it.id == id } ?: return
        editor.value = FocusTemplateEditorState(
            templateId = template.id,
            name = template.name,
            focusMinutesText = template.focusMinutes.toString(),
            breakMinutesText = template.breakMinutes.toString(),
        )
    }

    /** Cierra el formulario sin guardar. */
    fun onDismissEditor() {
        editor.value = null
    }

    /** Actualiza el nombre (con su longitud máxima) y limpia su error. */
    fun onNameChange(value: String) {
        editor.update { it?.copy(name = value.take(FocusTemplateLimits.MAX_NAME_LENGTH), nameErrorRes = null) }
    }

    /** Actualiza los minutos de foco: solo dígitos y a lo sumo [MAX_MINUTES_DIGITS]. */
    fun onFocusMinutesChange(value: String) {
        editor.update { it?.copy(focusMinutesText = value.onlyDigits(), focusErrorRes = null) }
    }

    /** Actualiza los minutos de descanso: solo dígitos y a lo sumo [MAX_MINUTES_DIGITS]. */
    fun onBreakMinutesChange(value: String) {
        editor.update { it?.copy(breakMinutesText = value.onlyDigits(), breakErrorRes = null) }
    }

    /**
     * Valida el formulario y, si es válido, crea o edita la plantilla. Un segundo toque mientras
     * se guarda se ignora. Si la escritura falla el formulario queda abierto con un mensaje.
     */
    fun onSave() {
        val current = editor.value ?: return
        if (current.isSaving) return
        val focusMinutes = current.focusMinutesText.toIntOrNull() ?: 0
        val breakMinutes = current.breakMinutesText.toIntOrNull() ?: 0
        val validation = FocusTemplateValidator.validate(current.name, focusMinutes, breakMinutes)
        if (!validation.isValid) {
            editor.value = current.copy(
                nameErrorRes = validation.nameError?.let(::nameErrorRes),
                focusErrorRes = validation.focusError?.let { R.string.focus_error_focus_minutes },
                breakErrorRes = validation.breakError?.let { R.string.focus_error_break_minutes },
            )
            return
        }
        editor.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val result = if (current.templateId == null) {
                createTemplate(current.name, focusMinutes, breakMinutes)
            } else {
                editTemplate(current.templateId, current.name, focusMinutes, breakMinutes)
            }
            when (result) {
                is KronoResult.Success -> editor.value = null
                else -> {
                    editor.update { it?.copy(isSaving = false) }
                    message.value = R.string.focus_error_save
                }
            }
        }
    }

    /** Marca la plantilla [id] como la activa. */
    fun onActivate(id: String) {
        viewModelScope.launch { reportFailure(activateTemplate(id), R.string.focus_error_activate) }
    }

    /** Pide confirmar la eliminación de la plantilla [id]; no hace nada si es la única que queda. */
    fun onRequestDelete(id: String) {
        if (!uiState.value.canDelete) return
        pendingDeleteId.value = id
    }

    /** Cierra el diálogo de confirmación conservando la plantilla. */
    fun onDismissDelete() {
        pendingDeleteId.value = null
    }

    /** Elimina la plantilla pendiente de confirmación. */
    fun onConfirmDelete() {
        val id = pendingDeleteId.value ?: return
        pendingDeleteId.value = null
        viewModelScope.launch { reportFailure(deleteTemplate(id), R.string.focus_error_delete) }
    }

    /** Descarta el mensaje transitorio una vez mostrado. */
    fun onMessageShown() {
        message.value = null
    }

    private fun reportFailure(result: KronoResult<Unit>, @StringRes failureMessage: Int) {
        if (result is KronoResult.Error) message.value = failureMessage
    }

    @StringRes
    private fun nameErrorRes(error: FocusTemplateError): Int = when (error) {
        FocusTemplateError.NameTooLong -> R.string.focus_error_name_long
        else -> R.string.focus_error_name_blank
    }

    private fun String.onlyDigits(): String = filter { it.isDigit() }.take(MAX_MINUTES_DIGITS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val MAX_MINUTES_DIGITS = 3
    }
}
