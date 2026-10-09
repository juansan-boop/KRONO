package com.krono.core.ui.icons

import androidx.annotation.DrawableRes
import com.krono.core.ui.R

/**
 * Íconos de KRONO: Material Symbols Outlined (peso 400, 24px) como VectorDrawable en
 * `res/drawable/ic_*.xml`. Origen y licencia (Apache 2.0) en
 * `core-ui/licenses/APACHE-2.0-MaterialSymbols.txt`.
 *
 * Cada propiedad es el recurso del ícono: se dibuja con `Icon(painter = painterResource(...))`
 * y el color lo da el `tint` del `Icon`. Los que cambian de sentido en idiomas de derecha a
 * izquierda (flechas y salida) están marcados como `autoMirrored` en su vector.
 */
object KronoIcons {
    /** Flecha de "volver"; se invierte en RTL. */
    @DrawableRes val ArrowBack: Int = R.drawable.ic_arrow_back

    /** Flecha de "continuar"; se invierte en RTL. */
    @DrawableRes val ArrowForward: Int = R.drawable.ic_arrow_forward

    /** Calendario con un día marcado. */
    @DrawableRes val CalendarToday: Int = R.drawable.ic_calendar_today

    /** Círculo con una equis (requisito no cumplido). */
    @DrawableRes val Cancel: Int = R.drawable.ic_cancel

    /** Círculo con una marca de verificación (requisito cumplido). */
    @DrawableRes val CheckCircle: Int = R.drawable.ic_check_circle

    /** Lista de tareas con casillas. */
    @DrawableRes val Checklist: Int = R.drawable.ic_checklist

    /** Círculo con exclamación (mensaje de error). */
    @DrawableRes val Error: Int = R.drawable.ic_error

    /** Cuadrícula de cuatro bloques (inicio). */
    @DrawableRes val GridView: Int = R.drawable.ic_grid_view

    /** Puerta con flecha de salida (cerrar sesión); se invierte en RTL. */
    @DrawableRes val Logout: Int = R.drawable.ic_logout

    /** Sobre con marca de verificación (correo enviado). */
    @DrawableRes val MarkEmailRead: Int = R.drawable.ic_mark_email_read

    /** Círculo vacío (requisito pendiente). */
    @DrawableRes val RadioButtonUnchecked: Int = R.drawable.ic_radio_button_unchecked

    /** Engranaje de ajustes. */
    @DrawableRes val Settings: Int = R.drawable.ic_settings

    /** Marca de verificación dentro de un círculo abierto (operación completada). */
    @DrawableRes val TaskAlt: Int = R.drawable.ic_task_alt

    /** Ojo abierto (mostrar contraseña). */
    @DrawableRes val Visibility: Int = R.drawable.ic_visibility

    /** Ojo tachado (ocultar contraseña). */
    @DrawableRes val VisibilityOff: Int = R.drawable.ic_visibility_off
}
