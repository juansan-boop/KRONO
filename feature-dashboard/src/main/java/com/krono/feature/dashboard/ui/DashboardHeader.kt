package com.krono.feature.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.OnSurface
import com.krono.feature.dashboard.R

/**
 * Saludo de la pantalla de inicio (F-13 CA-1): "Hola, Alexa", o solo "Hola" si el perfil aún no
 * tiene nombre.
 */
@Composable
internal fun DashboardHeader(userName: String?, modifier: Modifier = Modifier) {
    val greeting = if (userName == null) {
        stringResource(R.string.dashboard_greeting)
    } else {
        stringResource(R.string.dashboard_greeting_named, userName)
    }
    Text(
        text = greeting,
        style = MaterialTheme.typography.headlineLarge,
        color = OnSurface,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
}
