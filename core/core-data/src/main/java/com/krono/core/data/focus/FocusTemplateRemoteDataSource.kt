package com.krono.core.data.focus

import kotlinx.coroutines.flow.Flow

/**
 * Fuente remota de las plantillas de enfoque en Cloud Firestore (F-10). Gestiona la colección
 * `users/{userId}/focusTemplates`.
 */
interface FocusTemplateRemoteDataSource {

    /** Observa las plantillas del usuario en tiempo real; si el listener falla, el flujo termina con ese error. */
    fun observeTemplates(userId: String): Flow<List<FocusTemplateDto>>

    /**
     * Lee una vez las plantillas del usuario. No espera al servidor más de un límite corto: sin red
     * o con red lenta lee de la caché local, que si está vacía devuelve una lista vacía.
     */
    suspend fun getTemplates(userId: String): List<FocusTemplateDto>

    /**
     * Escribe [upserts] (documentos completos) y elimina [deletedIds] en un solo lote atómico.
     * Suspende hasta la confirmación del servidor, que sin red no llega: quien lo llama debe
     * acotar esa espera (ver [FirestoreFocusTemplateRepository]).
     */
    suspend fun commit(userId: String, upserts: List<FocusTemplateDto>, deletedIds: List<String>)
}
