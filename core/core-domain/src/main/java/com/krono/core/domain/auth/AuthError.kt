package com.krono.core.domain.auth

/**
 * Errores de autenticación. Extienden [Exception] porque `KronoResult.Error`
 * transporta un `Throwable`; nunca se lanzan hacia la UI, solo viajan dentro de
 * `KronoResult`. La UI traduce cada tipo a un mensaje en español.
 */
sealed class AuthError : Exception() {
    /** Falta algún campo obligatorio. */
    data object EmptyFields : AuthError()

    /** El correo no tiene un formato válido. */
    data object InvalidEmail : AuthError()

    /** La contraseña no cumple la política; [requirements] indica qué falta. */
    data class WeakPassword(val requirements: PasswordRequirements) : AuthError()

    /** La confirmación no coincide con la contraseña. */
    data object PasswordsDoNotMatch : AuthError()

    /** Correo o contraseña incorrectos (no se distingue cuál, a propósito). */
    data object InvalidCredentials : AuthError()

    /** Ya existe una cuenta con ese correo. */
    data object EmailAlreadyRegistered : AuthError()

    /** No existe una cuenta con ese correo (recuperación de contraseña). */
    data object EmailNotRegistered : AuthError()

    /** Falla no prevista (base de datos, criptografía...). La UI muestra un mensaje genérico. */
    data class Unexpected(override val cause: Throwable?) : AuthError()
}
