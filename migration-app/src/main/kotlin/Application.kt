package com.quadient.migration

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.quadient.migration.api.Migration
import com.quadient.migration.dto.StatisticsResponse
import com.quadient.migration.route.jobModule
import com.quadient.migration.route.rootModule
import com.quadient.migration.route.scriptsModule
import com.quadient.migration.service.Settings
import com.quadient.migration.service.SettingsService
import com.quadient.migration.shared.DocumentObjectType
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            this.registerModule(JavaTimeModule())
        }
    }
    install(Koin) {
        slf4jLogger()
        modules(appModules(environment))
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
                        val (unsupported, supported) = documentObjects.partition { it.type === DocumentObjectType.Unsupported }

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