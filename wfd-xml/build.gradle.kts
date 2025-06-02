plugins {
    java
    `maven-publish`
    id("net.researchgate.release") version "3.0.2"
}

release {
    failOnCommitNeeded.set(false)
    failOnUpdateNeeded.set(false)
    failOnUnversionedFiles.set(false)
}

tasks.afterReleaseBuild {
    dependsOn("publish")
}