package com.krono.feature.profile.security

/**
 * Nivel de acción o tipo de limpieza seleccionado en la Zona de Seguridad (F-21).
 */
enum class SecurityActionType {
    /** Borrado de tareas y restablecimiento a valores por defecto conservando cuenta (CA-1). */
    RESET_DATA,

    /** Eliminación definitiva de datos en Firestore y cuenta en Firebase con reautenticación (CA-2). */
    DELETE_ACCOUNT,
}

/**
 * Eventos de un solo uso emitidos desde [SecurityZoneViewModel] hacia la UI.
 */
sealed interface SecurityZoneEvent {
    /** La configuración y datos se restablecieron con éxito (CA-1). */
    data object DataResetSuccess : SecurityZoneEvent

    /** La cuenta fue eliminada permanentemente; la app debe redirigir al login (CA-2). */
    data object AccountDeleted : SecurityZoneEvent

    /** Navegación hacia atrás (retorno al Hub de Ajustes). */
    data object NavigateBack : SecurityZoneEvent
}

/**
 * Error de una operación de la Zona de Seguridad, mostrado dentro del diálogo abierto.
 *
 * La UI lo traduce a copy en `strings.xml`; el ViewModel no contiene texto de interfaz.
 */
sealed interface SecurityZoneError {
    /** La contraseña de reautenticación es incorrecta. */
    data object WrongPassword : SecurityZoneError

    /** No hay conexión: la operación no se inició y no se modificó ningún dato. */
    data object NetworkUnavailable : SecurityZoneError

    /** El servidor no confirmó la operación a tiempo. */
    data object Timeout : SecurityZoneError

    /**
     * Error no previsto. No transporta el texto de la excepción: es técnico, está en inglés y puede
     * contener detalles internos; la UI muestra copy genérico y el detalle va solo a logcat.
     */
    data object Unexpected : SecurityZoneError
}

/**
 * Estado inmutable de la pantalla de Zona de Seguridad (F-21 CA-1, CA-2).
 *
 * @property selectedAction Acción seleccionada en el paso 1 (Restablecer o Eliminar).
 * @property showResetConfirmationDialog Controla la visibilidad del diálogo de confirmación del paso 2 (CA-1).
 * @property showDeleteAccountDialog Controla la visibilidad del diálogo con reautenticación del paso 2 (CA-2).
 * @property isResettingData Indica si la purga de datos y restauración está en curso.
 * @property isDeletingAccount Indica si la reautenticación y eliminación de cuenta está en curso.
 * @property reauthPassword Contraseña ingresada para la reautenticación sensible de Firebase (CA-2).
 * @property isReauthPasswordEmpty Indica que se intentó confirmar con la contraseña vacía.
 * @property isPasswordVisible Alterna la visibilidad en texto claro de la contraseña.
 * @property error Error de la última operación, visible dentro del diálogo abierto; `null` si no hay.
 * @property isResetSuccessful Indica que el restablecimiento de datos terminó con éxito.
 */
data class SecurityZoneUiState(
    val selectedAction: SecurityActionType = SecurityActionType.RESET_DATA,
    val showResetConfirmationDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val isResettingData: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val reauthPassword: String = "",
    val isReauthPasswordEmpty: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: SecurityZoneError? = null,
    val isResetSuccessful: Boolean = false,
) {
    /**
     * Indica si hay alguna operación sensible en curso en segundo plano.
     */
    val isOperationInProgress: Boolean
        get() = isResettingData || isDeletingAccount
}
