package com.krono.feature.profile.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Comprueba si el dispositivo tiene conexión a internet antes de iniciar operaciones
 * remotas que no pueden quedar a medias (F-21).
 */
fun interface ConnectivityChecker {

    /** @return `true` si hay una red activa con capacidad de internet. */
    fun isOnline(): Boolean
}

/**
 * Implementación de [ConnectivityChecker] respaldada por [ConnectivityManager].
 *
 * Con el modo avión no hay red activa, por lo que devuelve `false` de inmediato.
 */
class AndroidConnectivityChecker(private val context: Context) : ConnectivityChecker {

    override fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
