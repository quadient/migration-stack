import org.gradle.internal.os.OperatingSystem;

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

    implementation("org.apache.groovy:groovy-all:5.0.0-rc-1")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

// Distribution build
tasks {
    val workDir = "${layout.buildDirectory.get()}/tmp/dist"
    val distDir = "${layout.buildDirectory.get()}/dist"

    shadowJar {
        destinationDirectory = File(workDir)
        archiveFileName = "app.jar"
        exclude("application*.conf")
    }

    buildFatJar {
        finalizedBy("buildFrontend", "copyScripts", "copyDist")
    }

    register<Copy>("copyScripts") {
        from("${rootProject.rootDir.parent}/migration-examples/src/main/groovy/com/quadient/migration/example") {
            include("**/*.groovy")
        }
        into("$workDir/modules")
    }

    register<Copy>("copyDist") {
        from("dist")
        into(workDir)
    }

    register<Exec>("installFrontendDependencies") {
        workingDir = File("${rootProject.rootDir}/web")

        if (OperatingSystem.current().isWindows) {
            commandLine("npm.cmd", "install")
        } else {
            commandLine("npm", "install")
        }
    }

    register<Exec>("buildFrontend") {
        workingDir = File("${rootProject.rootDir}/web")

        if (OperatingSystem.current().isWindows) {
            commandLine("npm.cmd", "run", "build")
        } else {
            commandLine("npm", "run", "build")
        }

        dependsOn("installFrontendDependencies")
    }

    register<Copy>("copyFrontend") {
        from("web/dist")
        into("$workDir/web")

        dependsOn("buildFrontend")
    }


    register<Zip>("buildDistZip") {
        from(workDir) {
            into("migration-app-${project.version}")
        }
        archiveFileName = "migration-app-${project.version}.zip"
        destinationDirectory = File(distDir)
        group = "distribution"

        dependsOn("buildFatJar", "copyDist", "copyScripts", "buildFrontend", "copyFrontend")
    }
}