package com.krono.core.domain.auth

/**
 * Validación de formato de correo en Kotlin puro (sin `android.util.Patterns`)
 * para poder probarla en el dominio.
 */
object EmailValidator {

    private val FORMATO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun isValid(email: String): Boolean = FORMATO.matches(email.trim())

    /** Forma canónica con la que se guarda y se busca un correo. */
    fun normalize(email: String): String = email.trim().lowercase()
}
