package com.quadient.migration

import com.quadient.migration.logging.Logging
import com.quadient.migration.service.GroovyService
import com.quadient.migration.service.ScriptDiscoveryService
import com.quadient.migration.service.SettingsService
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun appModules(env: ApplicationEnvironment) = module {
    single<ApplicationConfig> { env.config }
    singleOf(::SettingsService)
    singleOf(::ScriptDiscoveryService)
    singleOf(::GroovyService)
    singleOf(::Logging)
}