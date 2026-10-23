package com.krono.core.data.assistant

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.AssistantError
import com.krono.core.domain.assistant.PriorityExplanationGenerator
import com.krono.core.domain.assistant.PriorityPlan
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * Redacta la explicación del asistente con Gemini a través de Firebase AI Logic (D-5).
 *
 * Firebase AI Logic llama al modelo con la configuración de `google-services.json`, así
 * que la app no guarda ninguna clave de API. Requiere habilitar la Gemini Developer API en
 * la consola de Firebase; si no está habilitada, no hay conexión o el modelo tarda más de
 * [TIMEOUT_SECONDS], devuelve [AssistantError] y la UI muestra el texto de plantilla.
 */
class FirebaseAiPriorityExplanationGenerator @Inject constructor(
    private val promptBuilder: PriorityPromptBuilder,
) : PriorityExplanationGenerator {

    // Perezoso: crear el modelo exige Firebase inicializado, y así las pruebas JVM no lo tocan.
    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig { temperature = TEMPERATURE },
        )
    }

    /** Pide la explicación de [plan] a Gemini y valida la respuesta. */
    override suspend fun explain(plan: PriorityPlan): KronoResult<String> = try {
        val text = withTimeout(TIMEOUT_SECONDS.seconds) {
            model.generateContent(promptBuilder.build(plan)).text
        }
        validate(text)
    } catch (timeout: TimeoutCancellationException) {
        KronoResult.Error(AssistantError.Unavailable(timeout))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        KronoResult.Error(AssistantError.Unavailable(error))
    }

    /** Parámetros del modelo y límites de la respuesta. */
    companion object {
        /**
         * Modelo de Gemini. Si Google lo retira, basta con cambiar este nombre (o moverlo a
         * Remote Config, como recomienda Firebase).
         */
        const val MODEL_NAME = "gemini-3.5-flash"

        /** Longitud máxima aceptada; las respuestas más largas se descartan. */
        const val MAX_EXPLANATION_LENGTH = 280

        /** Tiempo máximo de espera antes de usar la plantilla. */
        const val TIMEOUT_SECONDS = 10

        // Baja para que las explicaciones sean sobrias y apegadas a los datos.
        private const val TEMPERATURE = 0.3f

        /** Acepta [text] si no está vacío y respeta [MAX_EXPLANATION_LENGTH]. */
        internal fun validate(text: String?): KronoResult<String> {
            val trimmed = text?.trim().orEmpty()
            return if (trimmed.isEmpty() || trimmed.length > MAX_EXPLANATION_LENGTH) {
                KronoResult.Error(AssistantError.InvalidResponse)
            } else {
                KronoResult.Success(trimmed)
            }
        }
    }
}
