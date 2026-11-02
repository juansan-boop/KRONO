package com.krono.core.data.timer

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Ruta del documento del cronómetro en curso (`users/{userId}/timer/active`, F-08), compartida
 * con la fuente de tareas para descartarlo al eliminar la tarea que mide.
 */
internal object TimerDocuments {
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_TIMER = "timer"
    private const val DOCUMENT_ACTIVE = "active"

    /** Documento del cronómetro en curso de [userId]. */
    fun activeTimer(firestore: FirebaseFirestore, userId: String): DocumentReference =
        firestore.collection(COLLECTION_USERS).document(userId)
            .collection(COLLECTION_TIMER).document(DOCUMENT_ACTIVE)
}
