@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration

import com.quadient.migration.api.Migration
import com.quadient.migration.dto.StatisticsResponse
import com.quadient.migration.route.rootModule
import com.quadient.migration.route.scriptsModule
import com.quadient.migration.service.*
import com.quadient.migration.shared.DocumentObjectType
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            explicitNulls = false
        })
    }
    install(Koin) {
        slf4jLogger()
        modules(appModules(environment))
    }

    val env = environment.config.getEnv()

    val settingsService by inject<SettingsService>()
    val scriptJobService by inject<ScriptJobService>()

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
            route("/job") {
                get {
                    val reqId = call.request.queryParameters["id"]
                    if (reqId == null) {
                        call.respondText("Missing job id", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val id = Uuid.parse(reqId)
                    val job = scriptJobService.get(JobId(id))

                    if (job == null) {
                        call.respondText("Job not found", status = HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respond(job.toResponse())
                }

                route("/list") {
                    get() {
                        val scriptPath = call.request.queryParameters["scriptPath"]
                        val jobs = if (scriptPath == null) {
                            scriptJobService.list()
                        } else {
                            scriptJobService.list().filter { it.path == scriptPath }
                        }

                        call.respond(jobs.map { it.toResponseWithoutLogs() })
                    }
                }
            }
        }
    }

    scriptsModule()
    rootModule()

    log.info("Server started in ${env.name} mode")
}

@Serializable
data class JobResponse(val id: String, val status: Status, val logs: List<String>?, val error: String?)

private fun Job.toResponse(): JobResponse {
    val id = this.id.toString()
    return when (this) {
        is Job.Running -> JobResponse(id, Status.RUNNING, logs, null)
        is Job.Success -> JobResponse(id, Status.SUCCESS, logs, null)
        is Job.Error -> JobResponse(id, Status.ERROR, logs, error)
    }
}

private fun Job.toResponseWithoutLogs(): JobResponse {
    val id = this.id.toString()
    return when (this) {
        is Job.Running -> JobResponse(id, Status.RUNNING, null, null)
        is Job.Success -> JobResponse(id, Status.SUCCESS, null, null)
        is Job.Error -> JobResponse(id, Status.ERROR, null, error)
    }
}

@Serializable
enum class Status {
    RUNNING, SUCCESS, ERROR
}