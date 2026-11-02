package com.krono.core.domain.focus

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [FocusTemplateRepository], compartido vía test fixtures.
 *
 * Aplica los cambios en bloque, como el lote atómico de la persistencia real.
 * - [failure]: si no es nulo, toda operación devuelve `KronoResult.Error(failure)`.
 * - [appliedChanges]: lotes recibidos, en orden, para verificar que cada operación es una sola escritura.
 */
class FakeFocusTemplateRepository(initial: List<FocusTemplate> = emptyList()) : FocusTemplateRepository {

    private val templates = MutableStateFlow(initial)

    /** Error forzado que devolverán las operaciones; `null` para funcionamiento normal. */
    var failure: Throwable? = null

    /** Lotes de cambios recibidos por [applyChanges], en orden. */
    val appliedChanges = mutableListOf<FocusTemplateChanges>()

    /** Veces que se pidió sembrar las plantillas por defecto. */
    var seedCalls = 0
        private set

    /** Plantillas actuales, para verificaciones. */
    val current: List<FocusTemplate> get() = templates.value

    override fun observeTemplates(): Flow<KronoResult<List<FocusTemplate>>> =
        templates.map { list -> failure?.let { KronoResult.Error(it) } ?: KronoResult.Success(list) }

    override suspend fun getTemplates(): KronoResult<List<FocusTemplate>> =
        failure?.let { KronoResult.Error(it) } ?: KronoResult.Success(templates.value)

    override suspend fun seedDefaultsIfEmpty(defaults: List<FocusTemplate>): KronoResult<Unit> {
        seedCalls++
        failure?.let { return KronoResult.Error(it) }
        if (templates.value.isEmpty()) templates.value = defaults
        return KronoResult.Success(Unit)
    }

    override suspend fun applyChanges(changes: FocusTemplateChanges): KronoResult<Unit> {
        appliedChanges += changes
        failure?.let { return KronoResult.Error(it) }
        val remaining = templates.value.filter { it.id !in changes.deletedIds }
        val replaced = remaining.map { existing -> changes.upserts.firstOrNull { it.id == existing.id } ?: existing }
        val added = changes.upserts.filter { upsert -> remaining.none { it.id == upsert.id } }
        templates.value = replaced + added
        return KronoResult.Success(Unit)
    }
}
