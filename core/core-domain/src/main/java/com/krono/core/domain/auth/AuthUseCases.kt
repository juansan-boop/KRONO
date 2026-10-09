package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Casos de uso de autenticación (F-22 a F-25). Kotlin puro: validan la entrada,
 * normalizan el correo y delegan en [AuthRepository]. Siempre devuelven
 * [KronoResult]; cualquier falla desconocida se entrega como [AuthError.Unexpected].
 *
 * No llevan `@Inject` porque `core-domain` no depende de javax.inject: se proveen
 * desde el módulo de Hilt de `core-data`.
 */
class LoginUseCase(private val repository: AuthRepository) {

    /** Inicia sesión; falla con [AuthError.EmptyFields] o [AuthError.InvalidEmail] sin llegar al repositorio. */
    suspend operator fun invoke(email: String, password: String): KronoResult<User> {
        if (email.isBlank() || password.isEmpty()) return KronoResult.Error(AuthError.EmptyFields)
        if (!EmailValidator.isValid(email)) return KronoResult.Error(AuthError.InvalidEmail)
        return safeCall { repository.login(EmailValidator.normalize(email), password) }
    }
}

/** Crea una cuenta tras validar campos, correo, política de contraseña y confirmación. */
class RegisterUseCase(private val repository: AuthRepository) {

    /** Registra la cuenta; devuelve el primer incumplimiento de validación como [AuthError] sin llegar al repositorio. */
    suspend operator fun invoke(email: String, password: String, confirmation: String): KronoResult<User> {
        if (email.isBlank() || password.isEmpty() || confirmation.isEmpty()) {
            return KronoResult.Error(AuthError.EmptyFields)
        }
        if (!EmailValidator.isValid(email)) return KronoResult.Error(AuthError.InvalidEmail)
        val requirements = PasswordPolicy.evaluate(password)
        if (!requirements.isSatisfied) return KronoResult.Error(AuthError.WeakPassword(requirements))
        if (password != confirmation) return KronoResult.Error(AuthError.PasswordsDoNotMatch)
        return safeCall { repository.register(EmailValidator.normalize(email), password) }
    }
}

/** Solicita el correo de recuperación de contraseña tras validar el correo. */
class ResetPasswordUseCase(private val repository: AuthRepository) {

    /** Pide el enlace de recuperación para [email]; falla con [AuthError.EmptyFields] o [AuthError.InvalidEmail]. */
    suspend operator fun invoke(email: String): KronoResult<Unit> {
        if (email.isBlank()) return KronoResult.Error(AuthError.EmptyFields)
        if (!EmailValidator.isValid(email)) return KronoResult.Error(AuthError.InvalidEmail)
        return safeCall { repository.requestPasswordReset(EmailValidator.normalize(email)) }
    }
}

/** Cierra la sesión. Sin UI por ahora: el botón vivirá en `feature-profile` (F-25 CA-4). */
class LogoutUseCase(private val repository: AuthRepository) {

    /** Cierra la sesión actual. */
    suspend operator fun invoke(): KronoResult<Unit> = safeCall { repository.logout() }
}

/** Sesión activa (o `null`). `app` la usa para decidir si arranca en el login o dentro de la app. */
class ObserveSessionUseCase(private val repository: AuthRepository) {

    /** Emite el usuario con sesión, o `null` cuando no hay sesión. */
    operator fun invoke(): Flow<User?> = repository.observeSession()
}

/** Convierte cualquier falla en un [AuthError], envolviendo las desconocidas en [AuthError.Unexpected]. */
fun Throwable.asAuthError(): AuthError = this as? AuthError ?: AuthError.Unexpected(this)

private inline fun <T> safeCall(block: () -> KronoResult<T>): KronoResult<T> {
    val result = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        KronoResult.Error(e)
    }
    return if (result is KronoResult.Error) KronoResult.Error(result.throwable.asAuthError()) else result
}
