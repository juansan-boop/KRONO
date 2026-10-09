package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticación (D-1). Lo implementa `core-data` con Firebase
 * Authentication; los casos de uso y la UI no conocen esa implementación.
 *
 * Los correos llegan ya normalizados (ver [EmailValidator.normalize]).
 * Ninguna operación lanza excepciones: los fallos viajan como
 * `KronoResult.Error` con un [AuthError].
 */
interface AuthRepository {

    /** Usuario con sesión activa, o `null` si no hay sesión. */
    fun observeSession(): Flow<User?>

    /**
     * Verifica credenciales e inicia sesión. Un correo inexistente y una contraseña
     * incorrecta devuelven el mismo [AuthError.InvalidCredentials].
     */
    suspend fun login(email: String, password: String): KronoResult<User>

    /** Crea la cuenta e inicia sesión. Error: [AuthError.EmailAlreadyRegistered]. */
    suspend fun register(email: String, password: String): KronoResult<User>

    /**
     * Envía el correo para restablecer la contraseña. Devuelve éxito aunque el
     * correo no tenga cuenta, para no revelar qué correos están registrados (D-10).
     */
    suspend fun requestPasswordReset(email: String): KronoResult<Unit>

    /** Cierra la sesión activa. */
    suspend fun logout(): KronoResult<Unit>
}
