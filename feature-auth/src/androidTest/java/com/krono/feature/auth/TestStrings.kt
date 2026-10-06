package com.krono.feature.auth

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** Lee el copy real de `strings.xml` para que las pruebas no dupliquen textos. */
fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
