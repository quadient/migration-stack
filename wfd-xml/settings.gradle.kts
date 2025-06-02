plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "wfd-xml"

include("api", "impl")
includeBuild("build-logic")

project(":api").name = "${rootProject.name}-api"
project(":impl").name = "${rootProject.name}-impl"