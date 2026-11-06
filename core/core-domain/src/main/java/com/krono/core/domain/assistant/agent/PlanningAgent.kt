package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult

/**
 * Modelo de lenguaje con herramientas que convierte un mensaje libre del usuario
 * ("tengo parcial el jueves, organízame la semana") en llamadas a las herramientas de
 * [PlanningAgentSession] (F-28).
 *
 * El modelo solo extrae tareas del mensaje y llama herramientas: la duración la calcula H1 y
 * el orden H2 dentro de la sesión, así que se mantiene la regla de D-5: el modelo no decide.
 */
interface PlanningAgent {

    /**
     * Atiende [request] llamando las herramientas de [session] las veces que haga falta.
     *
     * @return el mensaje final del modelo para el usuario (puede estar vacío), o
     * [KronoResult.Error] con un `AssistantError` si el servicio no responde.
     */
    suspend fun run(request: String, session: PlanningAgentSession): KronoResult<String>
}
