plugins {
    java
    `maven-publish`
    id("net.researchgate.release") version "3.0.2"
    id("org.owasp.dependencycheck") version "12.1.8"
}

release {
    failOnCommitNeeded.set(false)
    failOnUpdateNeeded.set(false)
    failOnUnversionedFiles.set(false)
}

tasks.afterReleaseBuild {
    dependsOn("publish")
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
