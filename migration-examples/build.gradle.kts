plugins {
    id("groovy")
    alias(libs.plugins.owasp.dependencycheck)
}

group = "com.quadient"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(1, TimeUnit.HOURS)
    }
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

apply {
    from(file("gradle/tasks.gradle.kts"))
}

dependencies {
    implementation("com.quadient:migration-library")

    implementation(libs.groovy.base)
    implementation(libs.groovy.xml)
    implementation(libs.groovy.toml)
    implementation(libs.groovy.sql)
    implementation(libs.groovy.json)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.postgresql)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.datetime)

    testRuntimeOnly(libs.junit.platform.launcher)

    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.21.5")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.5")
    }
}

tasks.test {
    useJUnitPlatform()
}
