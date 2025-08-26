package com.quadient.migration.route

import com.quadient.migration.api.Migration
import com.quadient.migration.logging.Logging
import com.quadient.migration.logging.ScriptIdMdcKey
import com.quadient.migration.logging.runWithMdc
import com.quadient.migration.service.GroovyResult
import com.quadient.migration.service.GroovyService
import com.quadient.migration.service.ScriptDiscoveryService
import com.quadient.migration.service.SettingsService
import com.quadient.migration.withPermitOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Application.scriptsModule() {
    val settingsService by inject<SettingsService>()
    val scriptDiscoveryService by inject<ScriptDiscoveryService>()
    val groovyService by inject<GroovyService>()
    val logging by inject<Logging>()

    val semaphore = Semaphore(1)

    routing {
        route("/api/scripts") {
            get {
                call.respond(scriptDiscoveryService.getScripts())
            }

            route("/run") {
                post {
                    val body = call.receive<RunScriptRequest>()
                    val scripts = scriptDiscoveryService.getScripts()
                    val script = scripts.firstOrNull() { it.path == body.path }

                    semaphore.withPermitOrElse(onUnavailable = {
                        call.respondText(
                            "Another script module is currently running, please try again later.",
                            status = HttpStatusCode.TooManyRequests
                        )
                        return@post
                    }) {
                        logging.clearLogsForScript(script!!.path) // TODO null assertion
                        runWithMdc(ScriptIdMdcKey, script.path) {
                            val migration = try {
                                Migration(
                                    settingsService.getSettings().migrationConfig,
                                    settingsService.getSettings().projectConfig
                                )
                            } catch (e: Exception) {
                                log.error("Failed to create Migration instance", e)
                                call.respondText(
                                    "Failed to create Migration instance: ${e.message}",
                                    status = HttpStatusCode.InternalServerError
                                )
                                return@runWithMdc
                            }

                            when (val result = groovyService.runScript(script, migration)) {
                                is GroovyResult.Ok -> {
                                    val logs = logging.getLogsForScript(script.path)
                                    call.respond(RunScriptResponse(logs))
                                }

                                is GroovyResult.Err -> {
                                    log.error("Script execution failed", result.ex)
                                    call.respondText(
                                        "Script execution failed: ${result.ex.message}",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                }
                            }
                        }
                    }
                }
            }

            route("/reload") {
                post {
                    try {
                        scriptDiscoveryService.loadScripts()
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        log.error("Failed to reload scripts", e)
                        call.respondText(
                            "Failed to reload scripts: ${e.message}", status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        }
    }
}

@Serializable
data class RunScriptRequest(val path: String)

@Serializable
data class RunScriptResponse(val logs: List<String>)
