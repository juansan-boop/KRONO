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
}
