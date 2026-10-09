package com.krono.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Aplicación de KRONO: punto de arranque de Hilt, que crea el grafo de dependencias
 * de toda la app.
 */
@HiltAndroidApp
class KronoApplication : Application()
