package com.krono.core.data.focus

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.krono.core.data.firestore.readFromServerOrCache
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [FocusTemplateRemoteDataSource] con Cloud Firestore (F-10), bajo la ruta
 * `users/{userId}/focusTemplates/{templateId}`, que ya cubren las reglas de seguridad de todo lo que cuelga de `users/{uid}`.
 */
class FirestoreFocusTemplateDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FocusTemplateRemoteDataSource {

    private fun templates(userId: String): CollectionReference =
        firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FOCUS_TEMPLATES)

    override fun observeTemplates(userId: String): Flow<List<FocusTemplateDto>> = callbackFlow {
        val listener = templates(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot.toDtos())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getTemplates(userId: String): List<FocusTemplateDto> {
        val collection = templates(userId)
        return readFromServerOrCache(
            fromServer = { collection.get().await() },
            fromCache = { collection.get(Source.CACHE).await() },
        ).toDtos()
    }

    // Un solo lote: o se aplican todos los cambios (activar una y desactivar otra) o ninguno.
    override suspend fun commit(userId: String, upserts: List<FocusTemplateDto>, deletedIds: List<String>) {
        val collection = templates(userId)
        val batch = firestore.batch()
        upserts.forEach { batch.set(collection.document(it.id), it.toMap()) }
        deletedIds.forEach { batch.delete(collection.document(it)) }
        batch.commit().await()
    }

    private fun QuerySnapshot?.toDtos(): List<FocusTemplateDto> =
        this?.documents?.map { FocusTemplateDto.fromMap(it.id, it.data.orEmpty()) }.orEmpty()

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FOCUS_TEMPLATES = "focusTemplates"
    }
}
