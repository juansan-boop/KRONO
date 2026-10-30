package com.krono.core.data.focus

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba en memoria de [FocusTemplateRemoteDataSource] que imita la caché local de
 * Firestore: el lote se aplica al instante y después espera [serverConfirmation], si existe, como
 * la confirmación del servidor (si nunca se completa, simula estar sin conexión).
 */
class FakeFocusTemplateRemoteDataSource : FocusTemplateRemoteDataSource {

    private val byUser = MutableStateFlow<Map<String, List<FocusTemplateDto>>>(emptyMap())

    /** Error con el que falla la escritura antes de aplicarse, como un error local. */
    var writeFailure: Exception? = null

    /** Error con el que falla la observación, como un `PERMISSION_DENIED` del listener. */
    var listenerFailure: Exception? = null

    /** Confirmación del servidor que esperan las escrituras; `null` para confirmar al instante. */
    var serverConfirmation: CompletableDeferred<Unit>? = null

    /** Lotes recibidos por [commit]: plantillas escritas y ids borrados, en orden. */
    val commits = mutableListOf<Pair<List<FocusTemplateDto>, List<String>>>()

    /** Plantillas actuales de [userId], para verificaciones. */
    fun templatesOf(userId: String): List<FocusTemplateDto> = byUser.value[userId].orEmpty()

    /** Precarga las plantillas de [userId]. */
    fun given(userId: String, templates: List<FocusTemplateDto>) {
        byUser.value = byUser.value + (userId to templates)
    }

    override fun observeTemplates(userId: String): Flow<List<FocusTemplateDto>> {
        val failure = listenerFailure
        return if (failure != null) flow { throw failure } else byUser.map { it[userId].orEmpty() }
    }

    override suspend fun getTemplates(userId: String): List<FocusTemplateDto> = templatesOf(userId)

    override suspend fun commit(userId: String, upserts: List<FocusTemplateDto>, deletedIds: List<String>) {
        writeFailure?.let { throw it }
        commits += upserts to deletedIds
        val remaining = templatesOf(userId).filter { it.id !in deletedIds }
        val replaced = remaining.map { existing -> upserts.firstOrNull { it.id == existing.id } ?: existing }
        val added = upserts.filter { upsert -> remaining.none { it.id == upsert.id } }
        given(userId, replaced + added)
        serverConfirmation?.await()
    }
}
