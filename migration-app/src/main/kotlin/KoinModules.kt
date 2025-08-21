package com.quadient.migration

import com.quadient.migration.service.ScriptDiscoveryService
import org.koin.dsl.module
import com.quadient.migration.service.SettingsService
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.tryGetString

fun appModules(env: ApplicationEnvironment) = module {
    single { SettingsService() }
    single { ScriptDiscoveryService(env.config.tryGetString("scripts-dir")) }
}