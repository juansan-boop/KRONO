package com.krono.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.app.R
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnError
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface

// TEMPORAL: se elimina cuando exista feature-profile (F-19); ver CierreSesionTemporal.kt.

/**
 * Conecta la pestaña Ajustes provisional con [CierreSesionTemporalViewModel].
 *
 * @param alCerrarSesion se invoca cuando la sesión quedó cerrada, para volver al grafo de auth.
 */
@Composable
fun AjustesRoute(
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CierreSesionTemporalViewModel = hiltViewModel(),
) {
    val confirmacionVisible by viewModel.confirmacionVisible.collectAsState()
    val cerrando by viewModel.cerrando.collectAsState()
    val hayError by viewModel.hayError.collectAsState()
    LaunchedEffect(viewModel) { viewModel.sesionCerrada.collect { alCerrarSesion() } }

    PantallaAjustes(
        confirmacionVisible = confirmacionVisible,
        cerrando = cerrando,
        hayError = hayError,
        alSolicitarCierre = viewModel::solicitarCierre,
        alCancelarCierre = viewModel::cancelarCierre,
        alConfirmarCierre = viewModel::cerrarSesion,
        modifier = modifier,
    )
}

/**
 * Pestaña Ajustes provisional: "Próximamente" más el botón temporal "Cerrar sesión", que abre
 * el diálogo de confirmación cuando [confirmacionVisible] es verdadero.
 */
@Composable
fun PantallaAjustes(
    confirmacionVisible: Boolean,
    cerrando: Boolean,
    hayError: Boolean,
    alSolicitarCierre: () -> Unit,
    alCancelarCierre: () -> Unit,
    alConfirmarCierre: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SeccionProvisional(titulo = R.string.seccion_ajustes, modifier = modifier) {
        Spacer(Modifier.height(KronoSpacing.lg))
        if (hayError) {
            Text(
                text = stringResource(R.string.cerrar_sesion_error),
                modifier = Modifier.padding(bottom = KronoSpacing.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorColor,
                textAlign = TextAlign.Center,
            )
        }
        OutlinedButton(
            onClick = alSolicitarCierre,
            enabled = !cerrando,
            modifier = Modifier
                .fillMaxWidth()
                .height(KronoSizes.controlHeight),
            shape = MaterialTheme.shapes.small,
            border = ButtonDefaults.outlinedButtonBorder(enabled = !cerrando).copy(
                brush = SolidColor(OutlineVariant),
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
        ) {
            IconoCerrarSesion()
            Text(text = stringResource(R.string.cerrar_sesion), style = MaterialTheme.typography.titleMedium)
        }
    }
    if (confirmacionVisible) {
        DialogoCerrarSesion(alCancelar = alCancelarCierre, alConfirmar = alConfirmarCierre)
    }
}

/**
 * Diálogo "¿Cerrar sesión?" con las acciones Cancelar y Cerrar sesión. Sobre una base opaca se
 * aplica el vidrio para que Ajustes no se transparente detrás del texto.
 */
@Composable
fun DialogoCerrarSesion(
    alCancelar: () -> Unit,
    alConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = alCancelar) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(KronoShapes.medium)
                .background(SurfaceContainerHigh)
                .glassSurface()
                .padding(KronoSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.cerrar_sesion_dialogo_titulo),
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.cerrar_sesion_dialogo_mensaje),
                modifier = Modifier.padding(top = KronoSpacing.sm, bottom = KronoSpacing.lg),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = alConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KronoSizes.controlHeight),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor, contentColor = OnError),
            ) {
                IconoCerrarSesion()
                Text(text = stringResource(R.string.cerrar_sesion), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(KronoSpacing.sm))
            OutlinedButton(
                onClick = alCancelar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KronoSizes.controlHeight),
                shape = MaterialTheme.shapes.small,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(OutlineVariant),
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
            ) {
                Text(text = stringResource(R.string.cancelar), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Ícono decorativo de salida, con separación a su derecha para el texto del botón. */
@Composable
private fun IconoCerrarSesion() {
    Icon(
        painter = painterResource(KronoIcons.Logout),
        contentDescription = null,
        modifier = Modifier.size(KronoSizes.iconSmall),
    )
    Spacer(Modifier.width(KronoSpacing.sm))
}
