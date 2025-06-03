rootProject.name = "migration-stack"

include(":migration-examples")
include(":migration-library")
include(":wfd-xml")

project(":migration-examples").projectDir = file("migration-examples")
project(":migration-library").projectDir = file("migration-library")
project(":wfd-xml").projectDir = file("wfd-xml")