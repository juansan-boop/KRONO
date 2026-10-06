package com.krono.core.domain.auth

/**
 * Usuario autenticado. El nombre no forma parte de la cuenta: se pide en el
 * onboarding (F-26) y vivirá en el perfil.
 */
data class User(
    val id: String,
    val email: String,
)
