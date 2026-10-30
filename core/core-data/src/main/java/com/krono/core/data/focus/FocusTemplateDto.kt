package com.krono.core.data.focus

import com.krono.core.domain.focus.FocusTemplate

/**
 * Documento de una plantilla de enfoque en Cloud Firestore (`users/{uid}/focusTemplates/{id}`,
 * F-10). Traduce el modelo de dominio a primitivos que Firestore guarda tal cual.
 */
data class FocusTemplateDto(
    val id: String = "",
    val name: String = "",
    val focusMinutes: Int = 0,
    val breakMinutes: Int = 0,
    val isActive: Boolean = false,
) {
    /** Convierte el documento al modelo de dominio. */
    fun toDomain(): FocusTemplate = FocusTemplate(id, name, focusMinutes, breakMinutes, isActive)

    /** Serializa el documento a un mapa compatible con Cloud Firestore. */
    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_NAME to name,
        FIELD_FOCUS_MINUTES to focusMinutes,
        FIELD_BREAK_MINUTES to breakMinutes,
        FIELD_IS_ACTIVE to isActive,
    )

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_FOCUS_MINUTES = "focusMinutes"
        const val FIELD_BREAK_MINUTES = "breakMinutes"
        const val FIELD_IS_ACTIVE = "isActive"

        /** Crea el documento a partir del modelo de dominio. */
        fun fromDomain(template: FocusTemplate): FocusTemplateDto = with(template) {
            FocusTemplateDto(id, name, focusMinutes, breakMinutes, isActive)
        }

        /**
         * Lee el documento [id] desde el mapa de Firestore, que entrega los números como `Long`.
         * Un campo ausente o de otro tipo toma su valor neutro en vez de descartar la plantilla.
         */
        fun fromMap(id: String, map: Map<String, Any?>): FocusTemplateDto = FocusTemplateDto(
            id = id,
            name = map[FIELD_NAME] as? String ?: "",
            focusMinutes = (map[FIELD_FOCUS_MINUTES] as? Number)?.toInt() ?: 0,
            breakMinutes = (map[FIELD_BREAK_MINUTES] as? Number)?.toInt() ?: 0,
            isActive = map[FIELD_IS_ACTIVE] as? Boolean ?: false,
        )
    }
}
