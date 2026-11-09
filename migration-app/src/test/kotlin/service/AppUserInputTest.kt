package com.quadient.migration.service

import com.quadient.migration.getScriptInputTimeout
import io.ktor.server.config.MapApplicationConfig
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AppUserInputTest {
    private val jobId = JobId(UUID.randomUUID())
    private val pendingAnswers = PendingAnswers()
    private val questions = LinkedBlockingQueue<InputRequest>()

    private fun input(timeout: Duration = 5.seconds) = AppUserInput(jobId, pendingAnswers, timeout) { questions.put(it) }

    private fun <T> asking(call: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(call)

    private fun nextQuestion(): InputRequest = questions.poll(5, TimeUnit.SECONDS) ?: fail("No question was sent")

    private fun answer(question: InputRequest, value: String) {
        assertTrue(pendingAnswers.complete(jobId, question.questionId, InputAnswer.Value(value)))
    }

    private fun failureOf(result: CompletableFuture<*>): Throwable {
        return assertFailsWith<ExecutionException> { result.get(5, TimeUnit.SECONDS) }.cause ?: fail("No cause")
    }

    @Test
    fun `choose returns answer posted for the pending question`() {
        val result = asking { input().choose("Select file", listOf("a.csv", "b.csv")) }

        val question = nextQuestion()
        answer(question, "b.csv")

        assertEquals("b.csv", result.get(5, TimeUnit.SECONDS))
        assertEquals(InputKind.Choose, question.kind)
        assertEquals("Select file", question.message)
        assertEquals(listOf("a.csv", "b.csv"), question.options)
    }

    @Test
    fun `confirm is true only for yes`() {
        val yes = asking { input().confirm("Delete?") }
        answer(nextQuestion(), "yes")
        assertTrue(yes.get(5, TimeUnit.SECONDS))

        val no = asking { input().confirm("Delete?") }
        answer(nextQuestion(), "no")
        assertFalse(no.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun `chooseOrType maps empty answer to default`() {
        val result = asking { input().chooseOrType("Select structure", listOf("s1"), "vs-1") }

        val question = nextQuestion()
        answer(question, "")

        assertEquals("vs-1", result.get(5, TimeUnit.SECONDS))
        assertEquals("vs-1", question.default)
    }

    @Test
    fun `cancel throws Cancelled by user`() {
        val result = asking { input().choose("Select file", listOf("a.csv")) }

        assertTrue(pendingAnswers.complete(jobId, nextQuestion().questionId, InputAnswer.Cancelled))

        val failure = failureOf(result)
        assertIs<UserInputException>(failure)
        assertEquals("Cancelled by user", failure.message)
    }

    @Test
    fun `timeout throws and clears pending question`() {
        val failure = assertFailsWith<UserInputException> { input(1.seconds).choose("Select file", listOf("a.csv")) }

        assertEquals("No answer received within 1 seconds", failure.message)
        assertFalse(pendingAnswers.complete(jobId, nextQuestion().questionId, InputAnswer.Value("a.csv")))
    }

    @Test
    fun `answer with stale question id is rejected`() {
        val result = asking { input().choose("Select file", listOf("a.csv")) }
        val question = nextQuestion()

        assertFalse(pendingAnswers.complete(jobId, UUID.randomUUID(), InputAnswer.Value("a.csv")))
        assertFalse(result.isDone)

        answer(question, "a.csv")
        assertEquals("a.csv", result.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun `stream marker is one line and request round-trips`() {
        val request = InputRequest(
            UUID.randomUUID(), InputKind.ChooseOrType, "a;b \"quoted\"\nsecond line", listOf("x;y"), "d"
        )

        val marker = request.toStreamMarker(jobId)
        val json = marker.substringAfter(";request=")

        assertTrue(marker.startsWith("id=$jobId;result=input;request="))
        assertFalse(marker.contains("\n"))
        assertTrue(json.contains("\"kind\":\"chooseOrType\""))
        val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        assertEquals(request, mapper.readValue<InputRequest>(json))
    }

    @Test
    fun `timeout config defaults to 10 minutes`() {
        assertEquals(600.seconds, MapApplicationConfig().getScriptInputTimeout())
        assertEquals(5.seconds, MapApplicationConfig("script-input-timeout-seconds" to "5").getScriptInputTimeout())
    }
}
