package com.krono.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.krono.core.ui.theme.OnSurface

/** Pantalla provisional: se reemplaza por el grafo de cada feature cuando su responsable lo implemente. */
@Composable
fun SeccionProvisional(@StringRes titulo: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(titulo),
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurface,
        )
    }
}
