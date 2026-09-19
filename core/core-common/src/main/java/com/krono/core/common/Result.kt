package com.krono.core.common

/**
 * Wrapper genérico de resultado usado en todas las capas de dominio/datos
 * para evitar excepciones no controladas cruzando límites de módulo.
 */
sealed interface KronoResult<out T> {
    data class Success<T>(val data: T) : KronoResult<T>
    data class Error(val throwable: Throwable) : KronoResult<Nothing>
    data object Loading : KronoResult<Nothing>
}
