package com.krono.feature.profile.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga ligera y asíncrona de un [ImageBitmap] desde un [avatarUri] local
 * sin dependencias de librerías externas pesadas.
 *
 * Utiliza [BitmapFactory.decodeStream] sobre [Dispatchers.IO] optimizando el uso
 * de memoria mediante remuestreo proporcional (máximo 512px) y retiene el resultado
 * en memoria mientras el URI no cambie. Si la carga falla o el URI es nulo, retorna `null`.
 *
 * @param avatarUri Cadena representativa del URI local seleccionado por el usuario.
 * @return [ImageBitmap] decodificado o `null` si el URI es nulo, vacío o ilegible.
 */
@Composable
fun rememberAsyncAvatarBitmap(avatarUri: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(avatarUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUri) {
        if (avatarUri.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(avatarUri)
                // Primer pase: leer dimensiones sin alojar mapa de bits completo
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                // Calcular factor de submuestreo para evitar alto consumo de memoria
                var sampleSize = 1
                val targetSize = 512
                while (boundsOptions.outWidth / sampleSize > targetSize ||
                    boundsOptions.outHeight / sampleSize > targetSize
                ) {
                    sampleSize *= 2
                }

                // Segundo pase: decodificar con submuestreo
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull()
        }
        bitmap = loaded?.asImageBitmap()
    }

    return bitmap
}
