package com.krono.feature.profile.ui

import com.krono.feature.profile.domain.ProfileValidationException

/**
 * Causa de un fallo al guardar el perfil o las preferencias, expuesta en el estado de la pantalla.
 *
 * La UI la traduce a copy en `strings.xml`; el ViewModel no contiene texto de interfaz ni el mensaje
 * crudo de la excepción.
 */
enum class ProfileSaveError {
    /** Los datos introducidos no son válidos (campo obligatorio vacío o valor fuera de rango). */
    INVALID_DATA,

    /** Error no previsto; el detalle técnico solo se escribe en logcat. */
    UNEXPECTED,
}

/**
 * Traduce una excepción de guardado a un [ProfileSaveError] seguro de mostrar.
 *
 * @param operation Nombre de la operación, solo para el registro de los errores no previstos.
 */
internal fun Throwable.toProfileSaveError(operation: String): ProfileSaveError = when (this) {
    is ProfileValidationException -> ProfileSaveError.INVALID_DATA
    else -> {
        ProfileLog.failure(operation, this)
        ProfileSaveError.UNEXPECTED
    }
}
