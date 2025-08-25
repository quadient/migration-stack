package com.quadient.migration.route

import com.quadient.migration.api.Migration
import com.quadient.migration.service.GroovyResult
import com.quadient.migration.service.GroovyService
import com.quadient.migration.service.ScriptDiscoveryService
import com.quadient.migration.service.SettingsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.scriptsModule() {
    val settingsService by inject<SettingsService>()
    val scriptDiscoveryService by inject<ScriptDiscoveryService>()
    val groovyService by inject<GroovyService>()

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
                        return@post
                    }

                    when (val result = groovyService.runScript(script!!, migration)) {
                        is GroovyResult.Ok -> {
                            log.debug("stdout: '{}', stderr: '{}'", result.stdout, result.stderr)
                            call.respond(RunScriptResponse(result.stdout.lines(), result.stderr.lines()))
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
data class RunScriptResponse(val stdout: List<String>, val stderr: List<String>)
