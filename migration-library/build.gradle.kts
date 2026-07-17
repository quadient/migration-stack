plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
    `java-library`
    id("maven-publish")
    alias(libs.plugins.owasp.dependencycheck)
}

group = "com.quadient"
version = "0.0.3-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
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

koinCompiler {
    strictSafety = true
}

buildscript {
    repositories {
        mavenCentral()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.quadient"
            artifactId = "migration"
            version = version
            from(components["java"])
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.quadient.wfdxml:wfd-xml-api")
    implementation("com.quadient.wfdxml:wfd-xml-impl")

    api(libs.koin.core)
    implementation(libs.koin.annotations)

    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktoml.core)
    implementation(libs.ktoml.file)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.dataformat.toml)

    implementation(libs.okhttp)

    testImplementation(libs.mockk)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)

    constraints {
        // ktor CVE fixes
        implementation("org.springframework.security:spring-security-crypto:7.0.6")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.21.5")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.5")
    }
}

tasks.test {
    useJUnitPlatform()
}

