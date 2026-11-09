package com.quadient.migration.route

import com.quadient.migration.service.InputAnswer
import com.quadient.migration.service.JobId
import com.quadient.migration.service.PendingAnswers
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Application.jobInputModule() {
    val pendingAnswers by inject<PendingAnswers>()

    routing { jobInputRoutes(pendingAnswers) }
}

fun Route.jobInputRoutes(pendingAnswers: PendingAnswers) {
    post("/api/job/{id}/input") {
        val jobId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (jobId == null) {
            call.respondText("Invalid job id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val body = call.receive<InputAnswerRequest>()
        val answer = when {
            body.cancel -> InputAnswer.Cancelled
            body.answer != null -> InputAnswer.Value(body.answer)
            else -> {
                call.respondText("Either answer or cancel is required", status = HttpStatusCode.BadRequest)
                return@post
            }
        }

        if (pendingAnswers.complete(JobId(jobId), body.questionId, answer)) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respondText("No pending question with this id", status = HttpStatusCode.Conflict)
        }
    }
}

data class InputAnswerRequest(val questionId: UUID, val answer: String? = null, val cancel: Boolean = false)
