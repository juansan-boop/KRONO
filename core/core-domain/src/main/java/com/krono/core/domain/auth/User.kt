package com.krono.core.domain.auth

/**
 * Usuario autenticado. El nombre no forma parte de la cuenta: se pide en el
 * onboarding (F-26) y vivirá en el perfil.
 */
data class User(
    /** Identificador único de la cuenta. */
    val id: String,
    /** Correo con el que se registró la cuenta. */
    val email: String,
)
