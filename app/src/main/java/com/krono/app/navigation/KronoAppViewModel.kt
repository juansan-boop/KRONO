package com.krono.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import com.krono.feature.profile.data.ProfileRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Destino con el que arranca la app según la sesión persistida (F-22 a F-25 CA-6). */
enum class StartDestination { Loading, Auth, Home }

/**
 * Decide el destino inicial de la app a partir de la sesión persistida y
 * asegura el aprovisionamiento y sincronización automática del documento
 * de usuario en Cloud Firestore desde el primer segundo (D-1, F-21).
 */
@HiltViewModel
class KronoAppViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val profileRemoteDataSource: ProfileRemoteDataSource = NoOpProfileRemoteDataSource(),
    private val profileLocalStore: ProfileLocalStore = ProfileLocalStore(),
) : ViewModel() {

    init {
        viewModelScope.launch {
            observeSession().collect { user ->
                if (user != null) {
                    profileLocalStore.bindUser(user.id, user.email)
                    profileRemoteDataSource.syncFromRemote(user.id, profileLocalStore)
                }
            }
        }
    }

    /**
     * Se decide una sola vez con la primera lectura de la sesión. Los cambios
     * posteriores (login, registro) navegan mediante callbacks, para no saltarse
     * pantallas como "¡Cuenta creada!".
     */
    val startDestination: StateFlow<StartDestination> = flow {
        emit(if (observeSession().first() != null) StartDestination.Home else StartDestination.Auth)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartDestination.Loading)
}
