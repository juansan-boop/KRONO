package com.krono.core.domain.focus

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Observa las plantillas de enfoque (F-10). Antes de observar siembra las tres plantillas por
 * defecto si el usuario no tiene ninguna (CA-1). Emite la activa primero y el resto por nombre, y
 * garantiza en lo que se muestra que hay exactamente una activa aunque los datos, por un
 * conflicto entre dispositivos, tengan cero o varias.
 */
class ObserveFocusTemplatesUseCase(
    private val repository: FocusTemplateRepository,
) {
    /** Emite la lista ordenada o el error de la observación. */
    operator fun invoke(): Flow<KronoResult<List<FocusTemplate>>> = flow {
        // Un fallo al sembrar no impide observar: si es real (permisos), la observación lo informa.
        repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all)
        emitAll(repository.observeTemplates().map { result -> result.mapList(::withSingleActive) })
    }
}

/**
 * Crea una plantilla (F-10 CA-2). Queda inactiva, salvo que sea la primera, para no cambiar la
 * plantilla en uso sin que el usuario lo pida.
 *
 * @param newId genera el id del documento nuevo.
 */
class CreateFocusTemplateUseCase(
    private val repository: FocusTemplateRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    /** Valida los datos y guarda la plantilla; devuelve el primer error de validación si lo hay. */
    suspend operator fun invoke(name: String, focusMinutes: Int, breakMinutes: Int): KronoResult<Unit> {
        FocusTemplateValidator.validate(name, focusMinutes, breakMinutes).firstError?.let {
            return KronoResult.Error(it)
        }
        return withTemplates(repository) { existing ->
            val template =
                FocusTemplate(newId(), name.trim(), focusMinutes, breakMinutes, isActive = existing.isEmpty())
            repository.applyChanges(FocusTemplateChanges(upserts = listOf(template)))
        }
    }
}

/** Edita el nombre y los minutos de una plantilla (F-10 CA-2) sin cambiar cuál está activa. */
class EditFocusTemplateUseCase(
    private val repository: FocusTemplateRepository,
) {
    /** Valida los datos y reemplaza la plantilla [id]; [FocusTemplateError.TemplateNotFound] si no existe. */
    suspend operator fun invoke(id: String, name: String, focusMinutes: Int, breakMinutes: Int): KronoResult<Unit> {
        FocusTemplateValidator.validate(name, focusMinutes, breakMinutes).firstError?.let {
            return KronoResult.Error(it)
        }
        return withTemplates(repository) { templates ->
            val current = templates.firstOrNull { it.id == id }
                ?: return@withTemplates KronoResult.Error(FocusTemplateError.TemplateNotFound)
            val edited = current.copy(name = name.trim(), focusMinutes = focusMinutes, breakMinutes = breakMinutes)
            repository.applyChanges(FocusTemplateChanges(upserts = listOf(edited)))
        }
    }
}

/**
 * Elimina una plantilla (F-10 CA-2). Si era la activa, activa otra en la misma escritura; la
 * única plantilla que queda no se puede eliminar, para no dejar al usuario sin ninguna activa.
 */
class DeleteFocusTemplateUseCase(
    private val repository: FocusTemplateRepository,
) {
    /** Elimina la plantilla [id]. */
    suspend operator fun invoke(id: String): KronoResult<Unit> = withTemplates(repository) { templates ->
        val target = templates.firstOrNull { it.id == id }
            ?: return@withTemplates KronoResult.Error(FocusTemplateError.TemplateNotFound)
        val remaining = withSingleActive(templates.filter { it.id != id })
        when {
            remaining.isEmpty() -> KronoResult.Error(FocusTemplateError.CannotDeleteLastTemplate)
            !target.isActive -> repository.applyChanges(FocusTemplateChanges(deletedIds = listOf(id)))
            else -> repository.applyChanges(
                FocusTemplateChanges(upserts = listOf(remaining.first()), deletedIds = listOf(id)),
            )
        }
    }
}

/** Marca una plantilla como la activa (F-10 CA-2) y desactiva las demás en una sola escritura. */
class ActivateFocusTemplateUseCase(
    private val repository: FocusTemplateRepository,
) {
    /** Activa la plantilla [id]; no escribe nada si ya era la única activa. */
    suspend operator fun invoke(id: String): KronoResult<Unit> = withTemplates(repository) { templates ->
        val target = templates.firstOrNull { it.id == id }
            ?: return@withTemplates KronoResult.Error(FocusTemplateError.TemplateNotFound)
        val othersActive = templates.filter { it.isActive && it.id != id }
        if (target.isActive && othersActive.isEmpty()) {
            KronoResult.Success(Unit)
        } else {
            val changed = othersActive.map { it.copy(isActive = false) } + target.copy(isActive = true)
            repository.applyChanges(FocusTemplateChanges(upserts = changed))
        }
    }
}

/** Lee las plantillas actuales y ejecuta [block] con ellas; propaga el error de lectura. */
private suspend fun withTemplates(
    repository: FocusTemplateRepository,
    block: suspend (List<FocusTemplate>) -> KronoResult<Unit>,
): KronoResult<Unit> = when (val result = repository.getTemplates()) {
    is KronoResult.Success -> block(result.data)
    is KronoResult.Error -> result
    KronoResult.Loading -> KronoResult.Loading
}

private fun <T, R> KronoResult<T>.mapList(transform: (T) -> R): KronoResult<R> = when (this) {
    is KronoResult.Success -> KronoResult.Success(transform(data))
    is KronoResult.Error -> this
    KronoResult.Loading -> KronoResult.Loading
}

/**
 * Ordena por nombre y deja marcada como activa solo una: la primera activa y, si ninguna lo está,
 * la primera de la lista. Devuelve la activa primero y el resto por nombre.
 */
internal fun withSingleActive(templates: List<FocusTemplate>): List<FocusTemplate> {
    val byName = templates.sortedBy { it.name.lowercase() }
    val active = byName.firstOrNull { it.isActive } ?: byName.firstOrNull() ?: return emptyList()
    return listOf(active.copy(isActive = true)) +
        byName.filter { it.id != active.id }.map { it.copy(isActive = false) }
}
