plugins {
    kotlin("plugin.serialization") version "2.0.0"
    id("com.apollographql.apollo").version("4.0.0")
    application
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("com.github.ben-manes.caffeine:caffeine:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.apollographql.apollo:apollo-runtime")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.4")
    implementation("ch.qos.logback:logback-classic:1.4.0")
    testImplementation(kotlin("test"))
}

apollo {
    service("service") {
        packageName.set("com.silverest.remotty.server")
        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

application {
    mainClass.set("com.silverest.remotty.server.MainKt") // Fully qualified name of your main function's class
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17" // or "11" or "17" depending on your JDK
}

tasks.test {
    useJUnitPlatform()
}
