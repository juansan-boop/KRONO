package com.krono.core.domain.sync

/**
 * Estado explícito de sincronización entre el almacenamiento local y el servidor remoto (Firestore).
 */
enum class SyncStatus {
    /** La escritura ha sido confirmada exitosamente por el servidor remoto. */
    CONFIRMED,

    /** La escritura se aplicó localmente y está encolada esperando sincronización remota (offline / timeout). */
    PENDING_SYNC,

    /** La sincronización remota falló debido a un error de red o excepción técnica. */
    FAILED,

    /** La sincronización remota fue rechazada por el servidor (ej. permisos, reglas o validación). */
    REJECTED,
}
