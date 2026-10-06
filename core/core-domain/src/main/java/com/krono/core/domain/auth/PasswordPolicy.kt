package com.krono.core.domain.auth

/** Estado de cada requisito de contraseña; la UI lo usa para los indicadores en vivo (F-23 CA-1). */
data class PasswordRequirements(
    val minLength: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
) {
    val isSatisfied: Boolean get() = minLength && hasDigit && hasSpecialChar
}

/** Reglas de contraseña: mínimo 8 caracteres, al menos un número y un carácter especial. */
object PasswordPolicy {

    const val MIN_LENGTH = 8

    fun evaluate(password: String): PasswordRequirements = PasswordRequirements(
        minLength = password.length >= MIN_LENGTH,
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
    )
}
