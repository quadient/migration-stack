package com.quadient.migration

import io.ktor.server.config.*

enum class Env { DEV, PROD }

fun ApplicationConfig.getEnv() = when (propertyOrNull("ktor.env")?.getString()) {
    "dev" -> Env.DEV
    "prod" -> Env.PROD
    null -> Env.DEV
    else -> throw IllegalArgumentException(
        "Unknown environment: ${propertyOrNull("ktor.env")?.getString()}"
    )
}