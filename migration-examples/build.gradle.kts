plugins {
    id("groovy")
    id("org.owasp.dependencycheck") version "12.1.3"
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
    from(file("gradle/dependencies.gradle"))
    from(file("gradle/tasks.gradle.kts"))
    plugin("org.owasp.dependencycheck")
}