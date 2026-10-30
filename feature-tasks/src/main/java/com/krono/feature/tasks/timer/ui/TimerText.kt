package com.krono.feature.tasks.timer.ui

import java.util.Locale
import kotlin.time.Duration

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L

/**
 * Formato de reloj del cronómetro: "MM:SS" por debajo de una hora y "H:MM:SS" desde ahí.
 * Son solo dígitos y separadores, así que no depende del idioma.
 */
internal fun formatClock(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
    val minutes = (totalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

private const val MILLIS_PER_SECOND = 1000L

/**
 * Formato de la cuenta regresiva del modo enfoque: siempre "MM:SS", con los minutos sin tope
 * (un bloque de una hora se ve "60:00") para que el reloj tenga un ancho estable. Redondea hacia
 * arriba, así "00:00" solo aparece cuando el tiempo de verdad se acabó.
 */
internal fun formatCountdown(remaining: Duration): String {
    val totalSeconds = ((remaining.inWholeMilliseconds + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}
