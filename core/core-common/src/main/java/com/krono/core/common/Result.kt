package com.krono.core.common

/**
 * Wrapper genérico de resultado usado en todas las capas de dominio/datos
 * para evitar excepciones no controladas cruzando límites de módulo.
 */
sealed interface KronoResult<out T> {
    /** Operación exitosa; [data] es el valor obtenido. */
    data class Success<T>(val data: T) : KronoResult<T>
    /** Operación fallida; [throwable] describe la causa y viaja sin lanzarse. */
    data class Error(val throwable: Throwable) : KronoResult<Nothing>
    /** Operación en curso, sin valor todavía. */
    data object Loading : KronoResult<Nothing>
}
