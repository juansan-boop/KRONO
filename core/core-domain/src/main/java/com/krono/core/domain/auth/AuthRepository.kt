package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticación (D-1). Hoy lo implementa `core-data` con Room;
 * en un hito futuro se reemplaza por un backend sin tocar casos de uso ni UI.
 *
 * Los correos llegan ya normalizados (ver [EmailValidator.normalize]).
 * Ninguna operación lanza excepciones: los fallos viajan como
 * `KronoResult.Error` con un [AuthError].
 */
interface AuthRepository {

    /** Usuario con sesión activa, o `null` si no hay sesión. */
    fun observeSession(): Flow<User?>

    /** Verifica credenciales e inicia sesión. Error: [AuthError.InvalidCredentials]. */
    suspend fun login(email: String, password: String): KronoResult<User>

    /** Crea la cuenta e inicia sesión. Error: [AuthError.EmailAlreadyRegistered]. */
    suspend fun register(email: String, password: String): KronoResult<User>

    /**
     * Solicita restablecer la contraseña. Mientras no haya backend es simulado:
     * no envía ningún correo. Error: [AuthError.EmailNotRegistered].
     */
    suspend fun requestPasswordReset(email: String): KronoResult<Unit>

    /** Cierra la sesión activa. */
    suspend fun logout(): KronoResult<Unit>
}
