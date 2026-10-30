package com.krono.core.domain.focus

/**
 * Plantillas que se siembran en una instalación nueva (F-10 CA-1). Sus ids son fijos para que
 * sembrarlas dos veces escriba los mismos documentos en vez de duplicarlos.
 */
object DefaultFocusTemplates {
    /** Plantilla activa por defecto: 45 minutos de foco y 10 de descanso. */
    val deepStudy = FocusTemplate("default-deep-study", "Estudio Profundo", 45, 10, isActive = true)

    /** Ciclo corto: 25 minutos de foco y 5 de descanso. */
    val quickSprint = FocusTemplate("default-quick-sprint", "Sprint Rápido", 25, 5, isActive = false)

    /** Ciclo largo: 60 minutos de foco y 15 de descanso. */
    val nightReading = FocusTemplate("default-night-reading", "Lectura Nocturna", 60, 15, isActive = false)

    /** Las tres plantillas, con exactamente una activa. */
    val all: List<FocusTemplate> = listOf(deepStudy, quickSprint, nightReading)
}
