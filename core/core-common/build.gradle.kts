plugins {
    id("java-library")
    alias(libs.plugins.kotlin.android).apply(false) // no-op aquí; se deja explícito por consistencia del catálogo
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

// Módulo JVM puro: no tiene variante debug, así que `testDebugUnitTest` (el paso del CI) no existe.
// Este alias hace que el CI también ejecute las pruebas de este módulo.
tasks.register("testDebugUnitTest") {
    dependsOn(tasks.named("test"))
}
