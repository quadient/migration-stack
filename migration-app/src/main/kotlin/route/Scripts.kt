package com.quadient.migration.route

import com.quadient.migration.logging.Logging
import com.quadient.migration.service.*
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
    val scriptJobService by inject<ScriptJobService>()
    val logging by inject<Logging>()

    val semaphore = Semaphore(1)

    routing {
        route("/api/scripts") {
            get {
                call.respond(scriptDiscoveryService.getScripts())
            }

            route("/runs") {
                post {
                    val body = call.receive<RunScriptRequest>()
                    val scripts = scriptDiscoveryService.getScripts()
                    val script = scripts.firstOrNull { it.path == body.path }

                    if (script == null) {
                        call.respondText("Script not found", status = HttpStatusCode.NotFound)
                        return@post
                    }

                    call.respondOutputStream(contentType = ContentType.Text.Plain) {
                        val job = scriptJobService.create(script.path)
                        val writer = this.bufferedWriter()

                        val result = logging.capture(script.path, {
                            job.appendLog(it)
                            writer.write(it)
                            writer.write("\n")
                            writer.flush()
                        }) {
                            groovyService.runScript(script, settingsService.getSettings())
                        }

                        when (result) {
                            is RunScriptResult.Ok -> {
                                writer.write("result=success\n")
                                scriptJobService.store(job.success())
                            }

                            is RunScriptResult.Err -> {
                                val message = result.ex.message ?: "Unknown error"
                                log.error("Script execution failed", result.ex)
                                writer.write("result=error,error=$message\n")
                                scriptJobService.store(job.error(message))
                            }
                        }

                        writer.close()
                    }
                }
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
                        val job = scriptJobService.create(script.path)
                        val (result, logs) = logging.capture(script.path) {
                            groovyService.runScript(script, settingsService.getSettings())
                        }
                        job.appendLogs(logs)

                        when (result) {
                            is RunScriptResult.Ok -> {
                                scriptJobService.store(job.success())
                                call.respond(
                                    HttpStatusCode.OK, RunScriptResponse(
                                        jobId = job.id.toString(),
                                        result = ScriptResult.SUCCESS,
                                        logs = logs,
                                        error = null
                                    )
                                )
                            }

                            is RunScriptResult.Err -> {
                                val message = result.ex.message ?: "Unknown error"
                                log.error("Script execution failed", result.ex)
                                scriptJobService.store(job.error(message))
                                call.respond(
                                    HttpStatusCode.OK, RunScriptResponse(
                                        jobId = job.id.toString(),
                                        result = ScriptResult.ERROR,
                                        logs = logs,
                                        error = message
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
data class RunScriptResponse(val jobId: String, val result: ScriptResult, val logs: List<String>, val error: String?)

@Serializable
enum class ScriptResult {
    SUCCESS, ERROR
}