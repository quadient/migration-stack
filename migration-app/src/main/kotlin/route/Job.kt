@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.route

import com.quadient.migration.dto.toResponse
import com.quadient.migration.dto.toResponseWithoutLogs
import com.quadient.migration.service.JobId
import com.quadient.migration.service.ScriptId
import com.quadient.migration.service.ScriptJobService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Application.jobModule() {
    val scriptJobService by inject<ScriptJobService>()

    routing {
        route("/api/job") {
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
                get {
                    val moduleId = call.request.queryParameters["moduleId"]
                    val jobs = if (moduleId == null) {
                        scriptJobService.list()
                    } else {
                        val id = ScriptId(moduleId)
                        scriptJobService.list().filter { it.scriptId == id }
                    }

                    call.respond(jobs.map { it.toResponseWithoutLogs() })
                }
            }
        }
    }
}
