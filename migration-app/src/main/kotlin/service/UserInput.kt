package com.quadient.migration.service

import com.fasterxml.jackson.annotation.JsonValue
import com.quadient.migration.log
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

enum class InputKind(@get:JsonValue val json: String) {
    Confirm("confirm"), Choose("choose"), ChooseOrType("chooseOrType")
}

data class InputRequest(
    val questionId: UUID,
    val kind: InputKind,
    val message: String,
    val options: List<String>,
    val default: String?,
)

private val markerMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

fun InputRequest.toStreamMarker(jobId: JobId): String {
    return "id=$jobId;result=input;request=${markerMapper.writeValueAsString(this)}"
}

sealed interface InputAnswer {
    data class Value(val value: String) : InputAnswer
    data object Cancelled : InputAnswer
}

class UserInputException(message: String) : RuntimeException(message)

class PendingAnswers {
    private data class Pending(val questionId: UUID, val answer: CompletableFuture<InputAnswer>)

    private val pending = ConcurrentHashMap<JobId, Pending>()

    fun register(jobId: JobId, questionId: UUID): CompletableFuture<InputAnswer> {
        return CompletableFuture<InputAnswer>().also { pending[jobId] = Pending(questionId, it) }
    }

    fun complete(jobId: JobId, questionId: UUID, answer: InputAnswer): Boolean {
        val current = pending[jobId] ?: return false
        if (current.questionId != questionId || !pending.remove(jobId, current)) {
            return false
        }
        return current.answer.complete(answer)
    }

    fun remove(jobId: JobId) {
        pending.remove(jobId)
    }
}

class AppUserInput(
    private val jobId: JobId,
    private val pendingAnswers: PendingAnswers,
    private val timeout: Duration,
    private val sendQuestion: (InputRequest) -> Unit,
) {
    fun confirm(message: String): Boolean {
        return ask(InputKind.Confirm, message, emptyList(), null) == "yes"
    }

    fun choose(message: String, options: List<String>): String {
        return ask(InputKind.Choose, message, options, null)
    }

    fun chooseOrType(message: String, options: List<String>, default: String?): String {
        val answer = ask(InputKind.ChooseOrType, message, options, default)
        return if (answer.isEmpty() && default != null) default else answer
    }

    private fun ask(kind: InputKind, message: String, options: List<String>, default: String?): String {
        val request = InputRequest(UUID.randomUUID(), kind, message, options, default)
        val answer = pendingAnswers.register(jobId, request.questionId)
        try {
            log.info("Question: $message")
            sendQuestion(request)

            return when (val result = waitFor(answer)) {
                is InputAnswer.Value -> result.value.also { log.info("Answer: $it") }
                InputAnswer.Cancelled -> fail("Cancelled by user")
            }
        } finally {
            pendingAnswers.remove(jobId)
        }
    }

    private fun waitFor(answer: CompletableFuture<InputAnswer>): InputAnswer {
        return try {
            answer.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            fail("No answer received within ${timeout.inWholeSeconds} seconds")
        }
    }

    private fun fail(message: String): Nothing {
        log.warn(message)
        throw UserInputException(message)
    }
}
