package com.krono.app.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.app.R
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OutlineVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TEMPORAL: se elimina cuando existan F-01/F-19 (feature-profile).
// Para quitarlo: borrar este archivo, su prueba, el string `cerrar_sesion*` y la llamada
// a `BotonCerrarSesionTemporal` en PantallaProximoHito. El diálogo de confirmación definitivo
// (F-25 CA-4) vivirá en feature-profile.

/** Cierra la sesión sin confirmación y avisa a la UI para volver al login. */
@HiltViewModel
class CierreSesionTemporalViewModel @Inject constructor(
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _cerrando = MutableStateFlow(false)
    val cerrando: StateFlow<Boolean> = _cerrando.asStateFlow()

    private val _hayError = MutableStateFlow(false)
    val hayError: StateFlow<Boolean> = _hayError.asStateFlow()

    private val _sesionCerrada = Channel<Unit>(Channel.BUFFERED)

    /** Se emite una vez la sesión quedó limpia; la UI navega al login. */
    val sesionCerrada: Flow<Unit> = _sesionCerrada.receiveAsFlow()

    fun cerrarSesion() {
        if (_cerrando.value) return
        _cerrando.value = true
        _hayError.value = false
        viewModelScope.launch {
            when (logout()) {
                is KronoResult.Success -> _sesionCerrada.send(Unit)
                is KronoResult.Error -> _hayError.value = true
                KronoResult.Loading -> Unit
            }
            _cerrando.value = false
        }
    }
}

/** Botón "Cerrar sesión" para la pantalla provisional. [alCerrarSesion] navega al login. */
@Composable
fun BotonCerrarSesionTemporal(
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CierreSesionTemporalViewModel = hiltViewModel(),
) {
    val cerrando by viewModel.cerrando.collectAsState()
    val hayError by viewModel.hayError.collectAsState()
    LaunchedEffect(viewModel) { viewModel.sesionCerrada.collect { alCerrarSesion() } }

    if (hayError) {
        Text(
            text = stringResource(R.string.cerrar_sesion_error),
            style = MaterialTheme.typography.bodyMedium,
            color = ErrorColor,
        )
        Spacer(Modifier.height(KronoSpacing.sm))
    }
    OutlinedButton(
        onClick = viewModel::cerrarSesion,
        enabled = !cerrando,
        modifier = modifier
            .fillMaxWidth()
            .height(KronoSizes.controlHeight),
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedButtonBorder(enabled = !cerrando).copy(
            brush = SolidColor(OutlineVariant),
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Spacer(Modifier.width(KronoSpacing.sm))
        Text(text = stringResource(R.string.cerrar_sesion), style = MaterialTheme.typography.titleMedium)
    }
}
