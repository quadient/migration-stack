package com.quadient.migration

import com.quadient.migration.route.rootModule
import com.quadient.migration.service.ScriptDiscoveryService
import com.quadient.migration.service.Settings
import com.quadient.migration.service.SettingsService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.receive

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Koin) {
        slf4jLogger()
        modules(appModules(environment))
    }

    val env = environment.config.getEnv()

    val settingsService by inject<SettingsService>()
    val scriptDiscoveryService by inject<ScriptDiscoveryService>()

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

            route("/tasks") {
                get {
                    println("Received request for tasks")
                    call.respondText("Tasks API Endpoint")
                }
            }

            route("/scripts") {
                get {
                    call.respond(scriptDiscoveryService.scripts)
                }

                route("/reload") {
                    post {
                        try {
                            scriptDiscoveryService.loadScripts()
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            log.error("Failed to reload scripts", e)
                            call.respondText("Failed to reload scripts: ${e.message}", status = HttpStatusCode.InternalServerError)
                        }
                    }
                }
            }
        }
    }

    rootModule()

    log.info("Server started in ${env.name} mode")
}