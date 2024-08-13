plugins {
    kotlin("plugin.serialization") version "2.0.0"
    `java-library`
}

apply(plugin = "kotlin")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}