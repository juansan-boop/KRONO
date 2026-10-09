package com.krono.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TEMPORAL: se elimina cuando exista feature-profile (F-19). Para quitarlo: borrar este archivo,
// su prueba, `PantallaAjustes.kt` y los strings `cerrar_sesion*`. El diálogo definitivo
// ("Permanecer en KRONO" / "Cerrar Sesión") vivirá en feature-profile.

/**
 * Gestiona el cierre de sesión con confirmación: la UI solicita el cierre, el usuario
 * confirma o cancela, y al confirmar se invoca [LogoutUseCase] y se avisa para volver al login.
 */
@HiltViewModel
class CierreSesionTemporalViewModel @Inject constructor(
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _cerrando = MutableStateFlow(false)
    /** Verdadero mientras el cierre de sesión está en curso; la UI deshabilita el botón. */
    val cerrando: StateFlow<Boolean> = _cerrando.asStateFlow()

    private val _confirmacionVisible = MutableStateFlow(false)
    /** Verdadero mientras el diálogo de confirmación debe mostrarse. */
    val confirmacionVisible: StateFlow<Boolean> = _confirmacionVisible.asStateFlow()

    private val _hayError = MutableStateFlow(false)
    /** Verdadero si el último intento de cerrar sesión falló. */
    val hayError: StateFlow<Boolean> = _hayError.asStateFlow()

    private val _sesionCerrada = Channel<Unit>(Channel.BUFFERED)

    /** Se emite una vez la sesión quedó limpia; la UI navega al login. */
    val sesionCerrada: Flow<Unit> = _sesionCerrada.receiveAsFlow()

    /** Pide confirmación al usuario antes de cerrar la sesión. */
    fun solicitarCierre() {
        _confirmacionVisible.value = true
    }

    /** Descarta la confirmación sin cerrar la sesión. */
    fun cancelarCierre() {
        _confirmacionVisible.value = false
    }

    /** Cierra la sesión tras la confirmación; ignora la llamada si ya hay un cierre en curso. */
    fun cerrarSesion() {
        _confirmacionVisible.value = false
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
