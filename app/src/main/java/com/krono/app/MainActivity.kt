package com.krono.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.krono.core.ui.theme.KronoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Punto de entrada único de la app. La navegación entre feature-* se resuelve
 * aquí; ningún feature-* importa a otro directamente (ver reglas de arquitectura
 * en CLAUDE.md).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KronoTheme {
                // TODO: NavHost con las rutas de feature-auth, feature-onboarding,
                // feature-dashboard, feature-tasks, feature-calendar y feature-profile.
            }
        }
    }
}
