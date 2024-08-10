// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    kotlin("jvm") version "1.9.0"
}

project ("server") {
    apply(plugin = "kotlin")

    group = "com.silverest"
    version = "1.0-SNAPSHOT"
}