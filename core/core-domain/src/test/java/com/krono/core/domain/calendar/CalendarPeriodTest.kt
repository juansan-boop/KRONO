package com.krono.core.domain.calendar

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [CalendarPeriod]: límites, navegación y rejilla mensual (F-16 CA-1 y CA-2). */
class CalendarPeriodTest {

    @Test
    fun dia_ocupaSoloElDiaAncla() {
        val period = CalendarPeriod(CalendarViewMode.DAY, LocalDate(2026, 10, 30))

        assertEquals(LocalDate(2026, 10, 30), period.firstDay)
        assertEquals(LocalDate(2026, 10, 31), period.endExclusive)
        assertEquals(listOf(LocalDate(2026, 10, 30)), period.days)
    }

    @Test
    fun semana_empiezaEnLunesYTermina7DiasDespues() {
        // 2024-10-24 es jueves; la semana del diseño va del 21 al 27.
        val period = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2024, 10, 24))

        assertEquals(DayOfWeek.MONDAY, WEEK_START)
        assertEquals(LocalDate(2024, 10, 21), period.firstDay)
        assertEquals(LocalDate(2024, 10, 28), period.endExclusive)
        assertEquals(7, period.days.size)
        assertEquals(LocalDate(2024, 10, 27), period.days.last())
    }

    @Test
    fun semana_siElAnclaEsDomingo_perteneceALaSemanaQueTerminaEseDia() {
        val period = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2024, 10, 27))

        assertEquals(LocalDate(2024, 10, 21), period.firstDay)
    }

    @Test
    fun semana_siElAnclaEsLunes_esElPrimerDia() {
        val period = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2024, 10, 21))

        assertEquals(LocalDate(2024, 10, 21), period.firstDay)
    }

    @Test
    fun mes_incluyeSemanasCompletasConDiasDeMesesVecinos() {
        // Octubre de 2024 empieza en martes: el diseño muestra del 30 de septiembre al 3 de noviembre.
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2024, 10, 24))

        assertEquals(LocalDate(2024, 9, 30), period.firstDay)
        assertEquals(LocalDate(2024, 11, 4), period.endExclusive)
        assertEquals(5, period.weeks.size)
        assertTrue(period.weeks.all { it.size == 7 })
        assertEquals(35, period.days.size)
    }

    @Test
    fun mes_queEmpiezaEnDomingo_necesitaSeisSemanas() {
        // 1 de octubre de 2023 fue domingo: la primera semana arranca el lunes 25 de septiembre.
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2023, 10, 15))

        assertEquals(LocalDate(2023, 9, 25), period.firstDay)
        assertEquals(LocalDate(2023, 11, 6), period.endExclusive)
        assertEquals(6, period.weeks.size)
        assertEquals(LocalDate(2023, 10, 1), period.weeks.first().last())
    }

    @Test
    fun febreroBisiesto_incluyeElDia29() {
        // Febrero de 2024: jueves 1 a jueves 29; la rejilla va del 29 de enero al 3 de marzo.
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2024, 2, 10))

        assertEquals(LocalDate(2024, 1, 29), period.firstDay)
        assertEquals(LocalDate(2024, 3, 4), period.endExclusive)
        assertTrue(LocalDate(2024, 2, 29) in period.days)
        assertEquals(5, period.weeks.size)
    }

    @Test
    fun febreroNoBisiestoQueEmpiezaEnLunes_ocupaExactamenteCuatroSemanas() {
        // Febrero de 2021: del lunes 1 al domingo 28.
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2021, 2, 14))

        assertEquals(4, period.weeks.size)
        assertEquals(LocalDate(2021, 2, 1), period.firstDay)
        assertEquals(LocalDate(2021, 3, 1), period.endExclusive)
    }

    @Test
    fun mes_distingueLosDiasDelMesDeLosVecinos() {
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2024, 10, 24))

        assertFalse(period.isInAnchorMonth(LocalDate(2024, 9, 30)))
        assertTrue(period.isInAnchorMonth(LocalDate(2024, 10, 1)))
        assertTrue(period.isInAnchorMonth(LocalDate(2024, 10, 31)))
        assertFalse(period.isInAnchorMonth(LocalDate(2024, 11, 1)))
    }

    @Test
    fun navegacion_diaAvanzaYRetrocedeUnDia() {
        val period = CalendarPeriod(CalendarViewMode.DAY, LocalDate(2026, 10, 31))

        assertEquals(LocalDate(2026, 11, 1), period.next().anchor)
        assertEquals(LocalDate(2026, 10, 30), period.previous().anchor)
    }

    @Test
    fun navegacion_semanaSaltaSieteDiasYConservaElModo() {
        val period = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2026, 10, 30))

        assertEquals(LocalDate(2026, 11, 6), period.next().anchor)
        assertEquals(LocalDate(2026, 10, 23), period.previous().anchor)
        assertEquals(CalendarViewMode.WEEK, period.next().mode)
    }

    @Test
    fun navegacion_mesSaltaAlPrimerDiaDelMesVecinoSinDesbordar() {
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2026, 1, 31))

        assertEquals(LocalDate(2026, 2, 1), period.next().anchor)
        assertEquals(LocalDate(2025, 12, 1), period.previous().anchor)
    }

    @Test
    fun navegacion_siguienteYAnteriorSonInversosEnMes() {
        val period = CalendarPeriod(CalendarViewMode.MONTH, LocalDate(2026, 3, 1))

        assertEquals(period, period.next().previous())
    }

    @Test
    fun contiene_usaLimiteInclusivoAlInicioYExclusivoAlFinal() {
        val period = CalendarPeriod(CalendarViewMode.WEEK, LocalDate(2024, 10, 24))

        assertTrue(period.contains(LocalDate(2024, 10, 21)))
        assertTrue(period.contains(LocalDate(2024, 10, 27)))
        assertFalse(period.contains(LocalDate(2024, 10, 20)))
        assertFalse(period.contains(LocalDate(2024, 10, 28)))
    }
}
