repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.testcontainers.testcontainers-java")
        }
    }
}

plugins {
    id("build.groovy-module")
    id("build.published-module")
}

dependencies {
    implementation(project(":${rootProject.name}-api"))
    implementation(libs.logback.classic)
    implementation(libs.jackson.databind)
    testImplementation(libs.testcontainers.spock)
    testImplementation(libs.xmlunit.core)
    testImplementation(libs.cglib)
    testImplementation(libs.spock.core)
    testImplementation(libs.groovy.xml)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}


tasks.test {
    if (project.hasProperty("excludeTests")) {
        exclude(project.property("excludeTests").toString().split(","))
    }
}