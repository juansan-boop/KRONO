package com.krono.core.domain.focus

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Conjunto de cambios que el repositorio aplica en una sola escritura atómica, para que el
 * invariante "siempre exactamente una plantilla activa" no se rompa a medias.
 *
 * @property upserts plantillas que se crean o se reemplazan por completo.
 * @property deletedIds ids de las plantillas que se eliminan.
 */
data class FocusTemplateChanges(
    val upserts: List<FocusTemplate> = emptyList(),
    val deletedIds: List<String> = emptyList(),
)

/**
 * Contrato de persistencia de las plantillas de enfoque (F-10).
 *
 * Las escrituras no esperan indefinidamente al servidor: si la confirmación no llega a tiempo
 * (sin conexión) quedaron encoladas en la caché local y se informan como éxito. Los errores que sí
 * llegan (permiso denegado, datos inválidos) se devuelven como [KronoResult.Error].
 */
interface FocusTemplateRepository {

    /** Observa las plantillas del usuario en tiempo real; un fallo de la lectura llega como error. */
    fun observeTemplates(): Flow<KronoResult<List<FocusTemplate>>>

    /** Lee una vez las plantillas del usuario (servidor con límite de espera, luego caché local). */
    suspend fun getTemplates(): KronoResult<List<FocusTemplate>>

    /**
     * Escribe [defaults] solo si el usuario no tiene ninguna plantilla (F-10 CA-1). Es idempotente:
     * usa los ids de [defaults], así repetirlo no duplica nada.
     */
    suspend fun seedDefaultsIfEmpty(defaults: List<FocusTemplate>): KronoResult<Unit>

    /** Aplica [changes] en una sola escritura atómica. */
    suspend fun applyChanges(changes: FocusTemplateChanges): KronoResult<Unit>
}
