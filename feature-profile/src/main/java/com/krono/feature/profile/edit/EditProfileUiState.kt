package com.krono.feature.profile.edit

import com.krono.feature.profile.ui.ProfileSaveError

/**
 * Eventos de un solo uso emitidos desde [EditProfileViewModel] hacia la capa de presentación.
 */
sealed interface EditProfileEvent {
    /** Los cambios se guardaron con éxito; navega hacia atrás o al destino previo. */
    data object ProfileSaved : EditProfileEvent

    /** El usuario canceló la edición; navega hacia atrás descartando cambios. */
    data object NavigateBack : EditProfileEvent
}

/**
 * Estado inmutable de la pantalla de Edición de Perfil (F-19 CA-2).
 *
 * Administra el formulario de edición bajo el Lumina Glass System, la lista de roles
 * académicos y profesionales seleccionables, validaciones de campos obligatorios
 * y estados de carga/error.
 *
 * @property name Nombre completo del usuario ingresado en el campo editable.
 * @property email Correo electrónico de la cuenta (modo informativo/lectura vinculado a Auth).
 * @property role Rol académico/profesional seleccionado por el usuario.
 * @property availableRoles Catálogo de roles seleccionables en la interfaz.
 * @property avatarUri URI local de la foto de perfil o `null` si se usan iniciales.
 * @property isNameMissing Indica que se intentó guardar con el nombre obligatorio vacío; la UI muestra el copy.
 * @property isSaving Indica si la operación de guardado se encuentra en curso (bloquea envíos concurrentes).
 * @property saveError Causa tipada del fallo de guardado (la UI la traduce a copy), o `null` si no hay.
 * @property isSuccess Indica si la persistencia se completó exitosamente.
 */
data class EditProfileUiState(
    val name: String = "Usuario",
    val email: String = "",
    val role: String = "Estudiante y trabajador",
    val availableRoles: List<String> = listOf(
        "Estudiante y trabajador",
        "Estudiante",
        "Trabajador tiempo completo",
        "Trabajador tiempo parcial",
    ),
    val avatarUri: String? = null,
    val isNameMissing: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: ProfileSaveError? = null,
    val isSuccess: Boolean = false,
) {
    /**
     * Iniciales calculadas a partir del nombre para el avatar con halo degradado.
     */
    val userInitials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "K" }

    /**
     * Determina si la acción de guardar está habilitada.
     */
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving
}
