plugins {
    id("build.repositories")
    id("build.java-module")
    groovy
}

tasks.test {
    useJUnitPlatform()
}