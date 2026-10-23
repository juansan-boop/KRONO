package com.krono.core.data.task

import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.domain.task.TaskError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [FirestoreTaskErrorMapper].
 */
class FirestoreTaskErrorMapperTest {

    @Test
    fun map_cuandoErrorEsTaskError_loRetornaDirectamente() {
        val error = TaskError.TaskNotFound
        val result = FirestoreTaskErrorMapper.map(error)
        assertEquals(TaskError.TaskNotFound, result)
    }

    @Test
    fun map_cuandoFirestoreNotFound_retornaTaskNotFound() {
        val firestoreException = FirebaseFirestoreException(
            "Document not found",
            FirebaseFirestoreException.Code.NOT_FOUND,
        )
        val result = FirestoreTaskErrorMapper.map(firestoreException)
        assertEquals(TaskError.TaskNotFound, result)
    }

    @Test
    fun map_cuandoFirestoreUnauthenticated_retornaUnauthenticatedUser() {
        val firestoreException = FirebaseFirestoreException(
            "Unauthenticated",
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
        )
        val result = FirestoreTaskErrorMapper.map(firestoreException)
        assertEquals(TaskError.UnauthenticatedUser, result)
    }

    @Test
    fun map_cuandoFirestorePermissionDenied_retornaStorageError() {
        val firestoreException = FirebaseFirestoreException(
            "Permission denied",
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
        )
        val result = FirestoreTaskErrorMapper.map(firestoreException)
        assertTrue(result is TaskError.StorageError)
    }

    @Test
    fun map_cuandoExcepcionGenerica_retornaStorageError() {
        val genericException = IllegalStateException("Error inesperado")
        val result = FirestoreTaskErrorMapper.map(genericException)
        assertTrue(result is TaskError.StorageError)
        assertEquals("Error al persistir la tarea: Error inesperado", result.message)
    }
}
