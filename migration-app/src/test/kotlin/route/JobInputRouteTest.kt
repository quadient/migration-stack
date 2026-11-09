package com.quadient.migration.route

import com.quadient.migration.service.InputAnswer
import com.quadient.migration.service.JobId
import com.quadient.migration.service.PendingAnswers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JobInputRouteTest {
    private val pendingAnswers = PendingAnswers()
    private val jobId = JobId(UUID.randomUUID())
    private val questionId = UUID.randomUUID()

    private fun inputRouteTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) { jackson() }
        routing { jobInputRoutes(pendingAnswers) }
        block()
    }

    private suspend fun ApplicationTestBuilder.postInput(jobId: String, body: String): HttpResponse {
        return client.post("/api/job/$jobId/input") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    @Test
    fun `answer for pending question completes it`() = inputRouteTest {
        val answer = pendingAnswers.register(jobId, questionId)

        val response = postInput(jobId.toString(), """{"questionId": "$questionId", "answer": "b.csv"}""")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(InputAnswer.Value("b.csv"), answer.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun `cancel completes with Cancelled`() = inputRouteTest {
        val answer = pendingAnswers.register(jobId, questionId)

        val response = postInput(jobId.toString(), """{"questionId": "$questionId", "cancel": true}""")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(InputAnswer.Cancelled, answer.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun `stale or duplicate answer is rejected`() = inputRouteTest {
        val answer = pendingAnswers.register(jobId, questionId)

        val wrongId = postInput(jobId.toString(), """{"questionId": "${UUID.randomUUID()}", "answer": "a.csv"}""")
        assertEquals(HttpStatusCode.Conflict, wrongId.status)
        assertFalse(answer.isDone)

        val first = postInput(jobId.toString(), """{"questionId": "$questionId", "answer": "a.csv"}""")
        val second = postInput(jobId.toString(), """{"questionId": "$questionId", "answer": "a.csv"}""")
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(HttpStatusCode.Conflict, second.status)
    }

    @Test
    fun `nothing pending is rejected`() = inputRouteTest {
        val response = postInput(UUID.randomUUID().toString(), """{"questionId": "$questionId", "answer": "a.csv"}""")

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `bad request`() = inputRouteTest {
        pendingAnswers.register(jobId, questionId)

        val badJobId = postInput("not-a-uuid", """{"questionId": "$questionId", "answer": "a.csv"}""")
        val noAnswer = postInput(jobId.toString(), """{"questionId": "$questionId"}""")

        assertEquals(HttpStatusCode.BadRequest, badJobId.status)
        assertEquals(HttpStatusCode.BadRequest, noAnswer.status)
    }
}
