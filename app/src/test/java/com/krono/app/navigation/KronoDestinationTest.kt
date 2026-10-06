package com.krono.app.navigation

import com.krono.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KronoDestinationTest {

    @Test
    fun laBarraTieneCuatroDestinosEnElOrdenInicioTareasCalendarioAjustes() {
        assertEquals(
            listOf(
                KronoDestination.Inicio,
                KronoDestination.Tareas,
                KronoDestination.Calendario,
                KronoDestination.Ajustes,
            ),
            KronoDestination.entries,
        )
    }

    @Test
    fun cadaDestinoTieneUnaRutaUnica() {
        val rutas = KronoDestination.entries.map { it.route }
        assertEquals(rutas.size, rutas.toSet().size)
    }

    @Test
    fun lasRutasSonLasAcordadas() {
        assertEquals(
            listOf("inicio", "tareas", "calendario", "ajustes"),
            KronoDestination.entries.map { it.route },
        )
    }

    @Test
    fun lasEtiquetasSonLasDelDiseno() {
        assertEquals(
            listOf(
                R.string.nav_inicio,
                R.string.nav_tareas,
                R.string.nav_calendario,
                R.string.nav_ajustes,
            ),
            KronoDestination.entries.map { it.labelRes },
        )
    }

    @Test
    fun elDestinoInicialEsInicio() {
        assertEquals(KronoDestination.Inicio, KronoDestination.Inicial)
    }

    @Test
    fun desdeRutaEncuentraElDestinoCorrespondiente() {
        assertEquals(KronoDestination.Calendario, KronoDestination.desdeRuta("calendario"))
    }

    @Test
    fun desdeRutaDevuelveNuloParaRutasDesconocidasONulas() {
        assertNull(KronoDestination.desdeRuta("auth"))
        assertNull(KronoDestination.desdeRuta(null))
    }
}
