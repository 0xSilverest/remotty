// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
    kotlin("jvm") version "2.0.10"
}

project ("server") {
    apply(plugin = "kotlin")

    group = "com.silverest"
    version = "1.0-SNAPSHOT"

    tasks.register<Copy>("installServerJar") {
        from(tasks.named("jar"))
        into(File(System.getProperty("user.home"), "bin"))
        doLast {
            println("Server JAR installed at ${System.getProperty("user.home")}/bin")
        }
    }
}
