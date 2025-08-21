val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.2.3"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.quadient.migration"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

tasks {
    register<JavaExec>("runServer") {
        mainClass = "io.ktor.server.netty.EngineMain"
        classpath = sourceSets["main"].runtimeClasspath
        group = "application"
        workingDir = project.rootDir
    }
}

dependencies {
    implementation("com.quadient:migration-library")
    implementation("com.quadient:migration-examples")

    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")

    implementation("io.ktor:ktor-server-core-jvm:${ktor_version}")
    implementation("io.ktor:ktor-server-netty:${ktor_version}")
    implementation("io.ktor:ktor-server-core:${ktor_version}")
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")

    implementation("ch.qos.logback:logback-classic:${logback_version}")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
