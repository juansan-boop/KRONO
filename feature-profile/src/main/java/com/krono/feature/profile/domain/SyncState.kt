package com.krono.feature.profile.domain

/**
 * Estado explícito de la sincronización del perfil y ajustes hacia Cloud Firestore (F-15, F-19, F-20).
 */
enum class SyncState {
    /** La última sincronización fue confirmada por el servidor remoto en la nube. */
    CONFIRMED,

    /** Cambios locales guardados exitosamente que se encuentran encolados o pendientes de sincronización remota (offline / timeout). */
    PENDING_SYNC,

    /** La sincronización remota fue rechazada por el servidor o falló técnicamente. */
    REJECTED,
}
