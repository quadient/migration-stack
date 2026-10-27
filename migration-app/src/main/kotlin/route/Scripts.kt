package com.quadient.migration.route

import com.quadient.migration.logging.Logging
import com.quadient.migration.service.*
import com.quadient.migration.tryWriteLine
import com.quadient.migration.withPermitOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.UUID

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
                    val body = call.receive<RunScriptsRequest>()
                    if (body.ids.isEmpty()) {
                        call.respondText("No scripts to run", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    val scripts = scriptDiscoveryService.getScripts()
                    val queue = body.ids.map { id ->
                        scripts.firstOrNull { it.id == id } ?: run {
                            call.respondText("Script $id not found", status = HttpStatusCode.NotFound)
                            return@post
                        }
                    }

                    stdinNotSupportedError(queue)?.let {
                        call.respondText(it, status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    semaphore.withPermitOrElse(onUnavailable = {
                        call.respondText(
                            "Another script module is currently running, please try again later.",
                            status = HttpStatusCode.TooManyRequests
                        )

                        return@post
                    }) {
                        call.respondOutputStream(contentType = ContentType.Text.Plain) {
                            val writer = this.bufferedWriter()
                            val runId = RunId(UUID.randomUUID())

                            for (script in queue) {
                                val job = scriptJobService.create(script.id, runId)
                                writer.tryWriteLine("id=${job.id};moduleId=${script.id};runId=$runId;result=running")

                                val result = logging.capture(script.id.toString(), {
                                    job.appendLog(it)
                                    writer.tryWriteLine(it)
                                }) {
                                    groovyService.dispatchScript(script, settingsService.getSettings())
                                }

                                when (result) {
                                    is RunScriptResult.Ok -> {
                                        writer.tryWriteLine("id=${job.id};result=success")
                                        scriptJobService.store(job.success())
                                    }

                                    is RunScriptResult.Err -> {
                                        val message = result.ex.message ?: "Unknown error"
                                        writer.tryWriteLine("id=${job.id};result=error;error=$message")
                                        scriptJobService.store(job.error(message))
                                        break
                                    }
                                }
                            }

                            writer.close()
                        }
                    }
                }
            }

            route("/run") {
                post {
                    val body = call.receive<RunScriptRequest>()
                    val scripts = scriptDiscoveryService.getScripts()
                    val script = scripts.firstOrNull { it.id == body.id }

                    if (script == null) {
                        call.respondText("Script not found", status = HttpStatusCode.NotFound)
                        return@post
                    }

                    stdinNotSupportedError(listOf(script))?.let {
                        call.respondText(it, status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    semaphore.withPermitOrElse(onUnavailable = {
                        call.respondText(
                            "Another script module is currently running, please try again later.",
                            status = HttpStatusCode.TooManyRequests
                        )

                        return@post
                    }) {
                        val job = scriptJobService.create(script.id, RunId(UUID.randomUUID()))
                        val (result, logs) = logging.capture(script.id.toString()) {
                            groovyService.dispatchScript(script, settingsService.getSettings())
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

// TODO remove when these modules get supported/reworked
private fun stdinNotSupportedError(scripts: List<ScriptMetadata>): String? {
    val names = scripts.filter { it.needsStdin }.map { it.displayName ?: it.filename }.distinct()
    if (names.isEmpty()) {
        return null
    }
    return "Modules that need user input cannot be executed from the app yet: ${
        names.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ", "
        )
    }"
}

@Serializable
data class RunScriptRequest(val id: ScriptId)

@Serializable
data class RunScriptsRequest(val ids: List<ScriptId>)

@Serializable
data class RunScriptResponse(val jobId: String, val result: ScriptResult, val logs: List<String>, val error: String?)

@Serializable
enum class ScriptResult {
    SUCCESS, ERROR
}