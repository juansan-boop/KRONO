plugins {
    id("java-library")
    // Plugin integrado de Gradle: expone dobles de prueba (p. ej. FakeAuthRepository)
    // para que los módulos que dependen del dominio los reutilicen en sus pruebas.
    id("java-test-fixtures")
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // KronoResult y Flow forman parte de la API pública del dominio (AuthRepository, casos de uso).
    api(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    testFixturesImplementation(project(":core:core-common"))
    testFixturesImplementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

// Módulo JVM puro: no tiene variante debug, así que `testDebugUnitTest` (el paso del CI) no existe.
// Este alias hace que el CI también ejecute las pruebas de este módulo.
tasks.register("testDebugUnitTest") {
    dependsOn(tasks.named("test"))
}
