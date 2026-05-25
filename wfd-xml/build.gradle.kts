plugins {
    java
    `maven-publish`
    alias(libs.plugins.release)
    alias(libs.plugins.owasp.dependencycheck)
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
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.testcontainers.testcontainers-java")
            }
        }
    }
}