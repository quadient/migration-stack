plugins {
    id("groovy")
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

apply {
    from("/gradle/dependencies.gradle")
    from("/gradle/tasks.gradle.kts")
}