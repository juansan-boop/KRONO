package com.krono.feature.tasks.timer.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Pruebas del formato de la cuenta regresiva del modo enfoque (F-09). */
class FormatCountdownTest {

    @Test
    fun muestraMinutosYSegundos() {
        assertEquals("31:49", formatCountdown(31.minutes + 49.seconds))
    }

    @Test
    fun unBloqueDeUnaHoraSeMuestraEnMinutos() {
        assertEquals("60:00", formatCountdown(60.minutes))
    }

    @Test
    fun redondeaHaciaArribaParaQueElCeroSoloAparezcaAlTerminar() {
        assertEquals("00:01", formatCountdown(100.milliseconds))
        assertEquals("00:00", formatCountdown(Duration.ZERO))
    }

    @Test
    fun unTiempoNegativoSeMuestraComoCero() {
        assertEquals("00:00", formatCountdown((-5).seconds))
    }
}
