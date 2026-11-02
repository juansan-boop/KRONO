package com.krono.core.domain.calendar

import com.krono.core.domain.timer.FakeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas de [CurrentDateTimeProvider], la fuente inyectable de "ahora" del calendario (F-16 CA-3). */
class CurrentDateTimeProviderTest {

    private val clock = FakeClock(Instant.parse("2026-10-30T15:30:00Z"))
    private val bogota = TimeZone.of("America/Bogota")

    @Test
    fun convierteElInstanteALaHoraLocalDeLaZonaIndicada() {
        val provider = CurrentDateTimeProvider(clock) { bogota }

        assertEquals(LocalDateTime(2026, 10, 30, 10, 30), provider.now())
        assertEquals(LocalDate(2026, 10, 30), provider.today())
    }

    @Test
    fun hoy_siguelaFechaLocalYNoLaDeUtc() {
        clock.current = Instant.parse("2026-10-31T03:00:00Z")
        val provider = CurrentDateTimeProvider(clock) { bogota }

        assertEquals(LocalDate(2026, 10, 30), provider.today())
    }

    @Test
    fun reflejaElAvanceDelReloj() {
        val provider = CurrentDateTimeProvider(clock) { TimeZone.UTC }
        clock.current = Instant.parse("2026-10-30T16:00:00Z")

        assertEquals(LocalDateTime(2026, 10, 30, 16, 0), provider.now())
    }
}
