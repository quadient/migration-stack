import org.gradle.internal.os.OperatingSystem;

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.owasp.dependencycheck)
}

dependencyCheck {
    format = "XML"
    nvd {
        datafeedUrl = "https://osquality-api.quadient.group/scan/api/v2/nvdcache"
        datafeedUser = "migration"
        datafeedPassword = System.getenv("NVD_PW")
    }
    analyzers {
        ossIndex {
            enabled = false
        }
    }
    skipConfigurations = listOf(
        "kotlinKlibCommonizerClasspath",
        "kotlinCompilerClasspath",
        "kotlinBuildToolsApiClasspath",
        "kotlinAbiValidationCompatClasspath",
    )
}

group = "com.quadient.migration"
version = "17.0.27"

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

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.serialization.jackson)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    // Update only after ktor migrates to kotlinx-serialization 1.9.0
    implementation(libs.kotlinx.datetime)

    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)

    implementation(libs.groovy.all)
    implementation(libs.stately.concurrent.collections)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    constraints {
        val nettyVersion = "4.2.16.Final"
        implementation("io.netty:netty-handler:${nettyVersion}")
        implementation("io.netty:netty-codec:${nettyVersion}")
        implementation("io.netty:netty-codec-http2:${nettyVersion}")
        implementation("io.netty:netty-codec-http3:${nettyVersion}")
        implementation("io.netty:netty-transport-native-kqueue:${nettyVersion}")
        implementation("io.netty:netty-transport-native-epoll:${nettyVersion}")
        implementation("io.netty:netty-tcnative:${nettyVersion}")
        implementation("io.netty:netty-tcnative-boringssl-static:${nettyVersion}")
    }
}

// Distribution build
tasks {
    val workDir = "${layout.buildDirectory.get()}/tmp/dist"
    val distDir = "${layout.buildDirectory.get()}/dist"

    register("printVersion") {
        group = "distribution"
        println(project.version)
    }

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
        into("$workDir/modules/com/quadient/migration/example")
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
