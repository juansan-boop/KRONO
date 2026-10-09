package com.krono.core.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.PasswordPolicy

/**
 * Traduce las excepciones de Firebase Authentication a [AuthError], para que
 * ningún tipo de Firebase salga de `core-data` (F-22 a F-25 CA-7).
 */
object FirebaseAuthErrorMapper {

    /** Código con el que Firebase marca un correo con formato inválido. */
    private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"

    /**
     * Devuelve el [AuthError] equivalente a [error]. Lo que no se reconoce queda
     * como [AuthError.Unexpected].
     *
     * @param attemptedPassword contraseña enviada, para indicar qué requisitos
     * faltan si Firebase la rechaza por débil.
     */
    fun map(error: Throwable, attemptedPassword: String = ""): AuthError = when (error) {
        is AuthError -> error
        is FirebaseNetworkException -> AuthError.NoConnection
        is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
        // Va antes que credenciales inválidas porque es una subclase suya.
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword(PasswordPolicy.evaluate(attemptedPassword))
        is FirebaseAuthInvalidCredentialsException -> mapInvalidCredentials(error)
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyRegistered
        // Usuario inexistente o deshabilitado: mismo mensaje que una contraseña incorrecta,
        // para no revelar qué correos tienen cuenta.
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
        else -> AuthError.Unexpected(error)
    }

    private fun mapInvalidCredentials(error: FirebaseAuthInvalidCredentialsException): AuthError =
        if (error.errorCode == ERROR_INVALID_EMAIL) AuthError.InvalidEmail else AuthError.InvalidCredentials
}
