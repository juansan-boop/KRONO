package com.krono.core.domain.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observa el nombre con el que se saluda al usuario (F-13 CA-1). Recorta los espacios y trata un
 * nombre en blanco como ausente, para que la UI nunca muestre "Hola, ".
 */
class ObserveUserNameUseCase(
    private val repository: UserNameRepository,
) {
    /** @return [Flow] con el nombre limpio, o `null` si no hay un nombre utilizable. */
    operator fun invoke(): Flow<String?> =
        repository.observeUserName().map { name -> name?.trim()?.takeIf { it.isNotEmpty() } }
}
