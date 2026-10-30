package com.krono.core.domain.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de prueba en memoria de [UserNameRepository]: asignar [name] simula un cambio del perfil. */
class FakeUserNameRepository : UserNameRepository {

    /** Nombre actual del usuario; `null` si aún no tiene. */
    val name = MutableStateFlow<String?>(null)

    override fun observeUserName(): Flow<String?> = name
}
