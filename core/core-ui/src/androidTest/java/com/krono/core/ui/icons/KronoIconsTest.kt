package com.krono.core.ui.icons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoTheme
import org.junit.Rule
import org.junit.Test

/** Cada ícono de [KronoIcons] debe existir y poder dibujarse como VectorDrawable. */
class KronoIconsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val iconos: Map<String, Int> = mapOf(
        "ArrowBack" to KronoIcons.ArrowBack,
        "ArrowForward" to KronoIcons.ArrowForward,
        "CalendarToday" to KronoIcons.CalendarToday,
        "Cancel" to KronoIcons.Cancel,
        "CheckCircle" to KronoIcons.CheckCircle,
        "Checklist" to KronoIcons.Checklist,
        "Error" to KronoIcons.Error,
        "GridView" to KronoIcons.GridView,
        "Logout" to KronoIcons.Logout,
        "MarkEmailRead" to KronoIcons.MarkEmailRead,
        "RadioButtonUnchecked" to KronoIcons.RadioButtonUnchecked,
        "Settings" to KronoIcons.Settings,
        "TaskAlt" to KronoIcons.TaskAlt,
        "Visibility" to KronoIcons.Visibility,
        "VisibilityOff" to KronoIcons.VisibilityOff,
    )

    @Test
    fun todosLosIconosSeDibujanComoVectores() {
        composeRule.setContent {
            KronoTheme {
                Column {
                    iconos.forEach { (nombre, recurso) ->
                        Icon(
                            painter = painterResource(recurso),
                            contentDescription = nombre,
                            modifier = Modifier.size(KronoSizes.iconMedium),
                        )
                    }
                }
            }
        }

        iconos.keys.forEach { nombre ->
            composeRule.onNodeWithContentDescription(nombre).assertIsDisplayed()
        }
    }
}
