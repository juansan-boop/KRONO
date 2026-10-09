package com.krono.feature.auth

import com.krono.core.domain.auth.AuthError

/**
 * Mensajes que la UI puede mostrar. Los ViewModels exponen este tipo (sin
 * recursos ni Compose) y las pantallas lo traducen a `strings.xml`. Así nunca
 * llega a la pantalla un mensaje técnico crudo.
 */
enum class AuthMessage {
    EmptyFields,
    InvalidEmail,
    WeakPassword,
    PasswordsDoNotMatch,
    InvalidCredentials,
    EmailAlreadyRegistered,
    NoConnection,
    TooManyRequests,
    Unexpected,
}

/** Traduce un error recibido en `KronoResult.Error` al mensaje que verá el usuario. */
fun Throwable.toAuthMessage(): AuthMessage = when (this) {
    AuthError.EmptyFields -> AuthMessage.EmptyFields
    AuthError.InvalidEmail -> AuthMessage.InvalidEmail
    is AuthError.WeakPassword -> AuthMessage.WeakPassword
    AuthError.PasswordsDoNotMatch -> AuthMessage.PasswordsDoNotMatch
    AuthError.InvalidCredentials -> AuthMessage.InvalidCredentials
    AuthError.EmailAlreadyRegistered -> AuthMessage.EmailAlreadyRegistered
    AuthError.NoConnection -> AuthMessage.NoConnection
    AuthError.TooManyRequests -> AuthMessage.TooManyRequests
    else -> AuthMessage.Unexpected
}
