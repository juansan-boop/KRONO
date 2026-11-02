package com.krono.feature.profile.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.feature.profile.domain.InvalidPasswordException
import com.krono.feature.profile.domain.NetworkUnavailableException
import com.krono.feature.profile.domain.RemoteTimeoutException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tiempo máximo de espera de una operación remota de la Zona de Seguridad.
 *
 * Firestore y Auth esperan la confirmación del servidor, que sin red nunca llega; pasado
 * este límite la operación falla con [RemoteTimeoutException] en lugar de quedar colgada.
 */
internal val REMOTE_OPERATION_TIMEOUT: Duration = 15.seconds

/**
 * Ejecuta [block] y lanza [RemoteTimeoutException] si no termina antes de [timeout].
 *
 * @param block Operación que no debe devolver `null`, pues `null` se interpreta como tiempo agotado.
 */
internal suspend fun <T : Any> withRemoteTimeout(
    timeout: Duration = REMOTE_OPERATION_TIMEOUT,
    block: suspend () -> T,
): T = withTimeoutOrNull(timeout) { block() } ?: throw RemoteTimeoutException()

/**
 * Traduce excepciones de Firebase a los errores del dominio de la Zona de Seguridad.
 *
 * Los errores no reconocidos se devuelven sin cambios.
 */
internal fun Throwable.toProfileOperationError(): Throwable = when {
    this is FirebaseAuthInvalidCredentialsException -> InvalidPasswordException()
    this is FirebaseNetworkException -> NetworkUnavailableException()
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        NetworkUnavailableException()
    else -> this
}
