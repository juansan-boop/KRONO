package com.krono.core.domain.auth

/** Estado de cada requisito de contraseña; la UI lo usa para los indicadores en vivo (F-23 CA-1). */
data class PasswordRequirements(
    /** Cumple la longitud mínima ([PasswordPolicy.MIN_LENGTH]). */
    val minLength: Boolean,
    /** Contiene al menos un dígito. */
    val hasDigit: Boolean,
    /** Contiene al menos un carácter especial (ni letra, ni dígito, ni espacio). */
    val hasSpecialChar: Boolean,
) {
    /** Verdadero cuando se cumplen todos los requisitos. */
    val isSatisfied: Boolean get() = minLength && hasDigit && hasSpecialChar
}

/** Reglas de contraseña: mínimo 8 caracteres, al menos un número y un carácter especial. */
object PasswordPolicy {

    /** Cantidad mínima de caracteres de una contraseña. */
    const val MIN_LENGTH = 8

    /** Evalúa [password] contra cada requisito de la política. */
    fun evaluate(password: String): PasswordRequirements = PasswordRequirements(
        minLength = password.length >= MIN_LENGTH,
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
    )
}
