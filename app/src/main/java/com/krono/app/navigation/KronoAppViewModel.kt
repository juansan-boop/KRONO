package com.krono.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.domain.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Destino con el que arranca la app según la sesión persistida (F-22 a F-25 CA-6). */
enum class StartDestination { Loading, Auth, Home }

@HiltViewModel
class KronoAppViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    /**
     * Se decide una sola vez con la primera lectura de la sesión. Los cambios
     * posteriores (login, registro) navegan mediante callbacks, para no saltarse
     * pantallas como "¡Cuenta creada!".
     */
    val startDestination: StateFlow<StartDestination> = flow {
        emit(if (observeSession().first() != null) StartDestination.Home else StartDestination.Auth)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartDestination.Loading)
}
