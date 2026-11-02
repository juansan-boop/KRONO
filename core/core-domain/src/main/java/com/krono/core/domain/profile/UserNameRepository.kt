package com.krono.core.domain.profile

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de lectura del nombre del usuario, que vive en su perfil (F-21). Existe para que
 * pantallas como el dashboard puedan saludar sin depender de `feature-profile`.
 */
interface UserNameRepository {

    /**
     * Observa el nombre de pila del usuario autenticado, o `null` si no hay sesión o aún no
     * tiene nombre. Un fallo de lectura se informa como `null`: el nombre es decorativo y no
     * debe impedir mostrar la pantalla.
     */
    fun observeUserName(): Flow<String?>
}
