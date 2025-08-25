package com.quadient.migration

import io.ktor.server.config.*

enum class Env { DEV, PROD }

fun ApplicationConfig.getEnv() = when (propertyOrNull("ktor.development")?.getAs<Boolean>()) {
    true -> Env.DEV
    false -> Env.PROD
    null -> Env.DEV
}

fun ApplicationConfig.getScriptDir() = tryGetString("scripts-dir") ?: "../migration-examples/src/main/groovy"
