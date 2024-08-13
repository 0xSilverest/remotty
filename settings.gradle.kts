pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.0")
            version("agp", "8.5.2")
            version("compose-compiler", "2.0.0")

            plugin("compose-compiler", "org.jetbrains.kotlin.plugin.compose").versionRef("kotlin")
        }
    }
}

rootProject.name = "remotty"
include(":app")
include(":server")
include(":common")
