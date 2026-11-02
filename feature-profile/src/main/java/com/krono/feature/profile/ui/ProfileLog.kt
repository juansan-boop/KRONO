package com.krono.feature.profile.ui

import android.util.Log

/**
 * Registro en logcat de los fallos de `feature-profile` que no se muestran al usuario.
 *
 * Solo escribe la operación y el tipo de la excepción: el mensaje de Firebase puede incluir rutas con
 * el identificador del usuario.
 */
internal object ProfileLog {
    private const val TAG = "KronoProfile"

    /** Registra que falló [operation] con una excepción de tipo [error]. */
    fun failure(operation: String, error: Throwable) {
        Log.w(TAG, "Fallo en $operation: ${error.javaClass.name}")
    }
}
