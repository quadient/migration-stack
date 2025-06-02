repositories {
    mavenCentral()
}

plugins {
    id("build.groovy-module")
    id("build.published-module")
}

dependencies {
    implementation(project(":${rootProject.name}-api"))
    implementation(libs.logback)
    testImplementation(libs.testcointainer.spock)
    testImplementation(libs.xmlunit.core)
    testImplementation(libs.cglib)
    testImplementation(libs.spock.core)
    testImplementation(libs.groovy.xml)
    testImplementation(libs.mockito.core)
}