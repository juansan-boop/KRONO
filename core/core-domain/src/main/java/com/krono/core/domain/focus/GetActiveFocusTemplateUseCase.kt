package com.krono.core.domain.focus

import com.krono.core.common.KronoResult

/**
 * Obtiene la plantilla de enfoque con la que arranca el modo enfoque (F-09 CA-1).
 *
 * Nunca falla: iniciar un enfoque no debe depender de que las plantillas se hayan sembrado o de
 * que se puedan leer. Sin plantillas, o si la lectura falla, usa la activa por defecto de
 * [DefaultFocusTemplates]; si ninguna está marcada activa, usa la primera.
 */
class GetActiveFocusTemplateUseCase(
    private val repository: FocusTemplateRepository,
) {
    /** Devuelve la plantilla activa del usuario o, en su defecto, la activa por defecto. */
    suspend operator fun invoke(): FocusTemplate {
        val templates = (repository.getTemplates() as? KronoResult.Success)?.data.orEmpty()
        return templates.firstOrNull { it.isActive } ?: templates.firstOrNull() ?: DefaultFocusTemplates.deepStudy
    }
}
