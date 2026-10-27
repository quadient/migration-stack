package com.quadient.migration

import com.github.ajalt.clikt.command.main
import com.quadient.migration.api.Migration
import com.quadient.migration.cli.migrationApp
import com.quadient.migration.dto.StatisticsResponse
import com.quadient.migration.route.jobModule
import com.quadient.migration.route.rootModule
import com.quadient.migration.route.scriptsModule
import com.quadient.migration.service.Settings
import com.quadient.migration.service.SettingsService
import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

suspend fun main(args: Array<String>) = migrationApp().main(args)

fun Application.module() {
    install(ContentNegotiation) {
        jackson()
    }
    install(Koin) {
        slf4jLogger()
        modules(appModules(environment.config))
    }

    val env = environment.config.getEnv()

    val settingsService by inject<SettingsService>()

    routing {
        route("/api") {
            route("/settings") {
                get {
                    log.debug("Received request to get settings")
                    call.respond(settingsService.getSettings())
                }

                post {
                    log.debug("Received request to save settings")
                    val settings = call.receive<Settings>()
                    settingsService.setSettings(settings)
                    call.respondText("Settings saved successfully")
                }
            }

            route("/statistics") {
                get {
                    val settings = settingsService.getSettings()

                    try {
                        val migration = Migration(settings.migrationConfig, settings.projectConfig)

                        val documentObjects = migration.documentObjectRepository.listAll()
                        val (unsupported, supported) = documentObjects.partition { it.skip.skipped }

                        call.respond(StatisticsResponse(unsupported.size, supported.size))
                    } catch (e: IllegalArgumentException) {
                        log.warn("Cannot create migration instance: ${e.message}")
                        call.respond(StatisticsResponse(null, null))
                    }
                }
            }
        }
    }

    jobModule()
    scriptsModule()
    rootModule()

    log.info("Server started in ${env.name} mode")
}