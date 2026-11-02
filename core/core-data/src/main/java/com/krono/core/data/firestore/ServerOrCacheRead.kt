package com.krono.core.data.firestore

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Espera máxima a que el servidor responda una lectura de comprobación antes de usar la caché
 * local. Con red, Firestore responde en mucho menos; con una red lenta, el usuario solo espera esto.
 */
internal val SERVER_READ_TIMEOUT: Duration = 2.seconds

/**
 * Lee un documento del servidor y, si no hay conexión o el servidor no responde antes de
 * [SERVER_READ_TIMEOUT], de la caché local.
 *
 * Un `get()` de Firestore sin límite puede tardar varios segundos con una red lenta y, sin red,
 * falla con `UNAVAILABLE` si el documento no está en caché. Estas lecturas solo comprueban el
 * estado antes de una acción del usuario, así que no deben bloquearla. Si la caché tampoco tiene
 * el documento, se propaga su error para que quien lee decida cómo degradar.
 *
 * @param fromServer lectura con la fuente por defecto (servidor, con las escrituras locales encima).
 * @param fromCache lectura solo de la caché local.
 */
internal suspend fun <T : Any> readFromServerOrCache(
    fromServer: suspend () -> T,
    fromCache: suspend () -> T,
): T = readFromServerWithinLimit(fromServer) ?: fromCache()

/** Lee del servidor; `null` si no hay conexión o si no responde a tiempo. */
private suspend fun <T : Any> readFromServerWithinLimit(fromServer: suspend () -> T): T? = try {
    withTimeoutOrNull(SERVER_READ_TIMEOUT) { fromServer() }
} catch (offline: FirebaseFirestoreException) {
    if (!offline.isUnavailable()) throw offline
    null
}

/** La operación falló por falta de conexión (o porque el documento no estaba en la caché local). */
internal fun Throwable.isUnavailable(): Boolean =
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE
