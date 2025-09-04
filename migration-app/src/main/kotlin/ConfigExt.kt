package com.quadient.migration

import io.ktor.server.config.*
import kotlin.io.path.Path

enum class Env { DEV, PROD }

fun ApplicationConfig.getEnv() = when (propertyOrNull("ktor.development")?.getAs<Boolean>()) {
    true -> Env.DEV
    false -> Env.PROD
    null -> Env.DEV
}

fun ApplicationConfig.getScriptDir() = tryGetString("scripts-dir") ?: "modules"
fun ApplicationConfig.getFeDir() = tryGetString("fe-dir") ?: "web"
fun ApplicationConfig.getAppDataDir() = tryGetString("app-data-dir") ?: Path("data", "app").toString()
fun ApplicationConfig.getModulesDataDir() = tryGetString("modules-data-dir") ?: Path("data", "modules").toString()
