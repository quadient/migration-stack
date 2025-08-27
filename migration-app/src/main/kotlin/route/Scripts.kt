package com.quadient.migration.route

import com.quadient.migration.logging.Logging
import com.quadient.migration.service.GroovyService
import com.quadient.migration.service.RunScriptResult
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
                    val script = scripts.firstOrNull { it.path == body.path }

                    if (script == null) {
                        call.respondText("Script not found", status = HttpStatusCode.NotFound)
                        return@post
                    }

                    semaphore.withPermitOrElse(onUnavailable = {
                        call.respondText(
                            "Another script module is currently running, please try again later.",
                            status = HttpStatusCode.TooManyRequests
                        )

                        return@post
                    }) {
                        val (result, logs) = logging.capture(script.path) {
                            groovyService.runScript(script, settingsService.getSettings())
                        }

                        when (result) {
                            is RunScriptResult.Ok -> {
                                call.respond(
                                    HttpStatusCode.OK, RunScriptResponse(
                                        result = ScriptResult.SUCCESS, logs = logs, error = null
                                    )
                                )
                            }

                            is RunScriptResult.Err -> {
                                log.error("Script execution failed", result.ex)
                                call.respond(
                                    HttpStatusCode.OK, RunScriptResponse(
                                        result = ScriptResult.ERROR,
                                        logs = logs,
                                        error = result.ex.message ?: "Unknown error"
                                    )
                                )
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
data class RunScriptResponse(val result: ScriptResult, val logs: List<String>, val error: String?)

@Serializable
enum class ScriptResult {
    SUCCESS, ERROR
}