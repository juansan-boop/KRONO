package com.krono.core.data.profile

import kotlinx.coroutines.flow.Flow

/** Fuente remota del nombre de pila del usuario, guardado en su documento de perfil `users/{userId}`. */
interface UserNameRemoteDataSource {

    /**
     * Observa el campo de nombre del perfil; emite `null` si el documento o el campo no existen.
     * Si el listener falla, el flujo termina con la excepción del SDK.
     */
    fun observeUserName(userId: String): Flow<String?>
}
