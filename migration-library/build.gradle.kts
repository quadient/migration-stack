val kotlinVersion: String by project
val logbackVersion: String by project
val postgresVersion: String by project
val exposedVersion: String by project
val mockkVersion = "1.13.13"

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("maven-publish")
    id("org.owasp.dependencycheck") version "12.1.8"
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
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.owasp:dependency-check-gradle:12.1.3")
    }
}

apply {
    plugin("org.owasp.dependencycheck")
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

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:11.12.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.12.0")

    // exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.akuleshov7:ktoml-core:0.6.0")
    implementation("com.akuleshov7:ktoml-file:0.6.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.18.2")

    testImplementation("io.mockk:mockk:${mockkVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
