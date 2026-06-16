package com.quadient.migration.service.deploy

import com.quadient.migration.api.EvolveConfig
import com.quadient.migration.api.InspireConfig
import com.quadient.migration.api.MigConfig
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfInstance
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException

class CaApiClientTest {
    private val httpClient = mockk<OkHttpClient>()
    private val evolveConfig = EvolveConfig(
        apiRetryDelayMs = 0,
        contentAuthorApiKey = "test-api-key",
        companyUrl = "https://ca.example.com",
        holder = "testHolder",
        holderType = "testHolderType",
        publishBlockActionId = "block-action",
        publishTemplateActionId = "template-action",
        publishRuleActionId = "rule-action",
    )
    private val migConfig = MigConfig(inspireConfig = InspireConfig(evolveConfig = evolveConfig))
    private val subject = CaApiClient(migConfig, httpClient)

    private val requestSlot = slot<Request>()
    private val mockCall = mockk<Call>()

    @BeforeEach
    fun setup() {
        every { httpClient.newCall(capture(requestSlot)) } returns mockCall
        every { mockCall.clone() } returns mockCall
        every { mockCall.request() } answers { requestSlot.captured }
    }

    @Nested
    inner class UploadResourceTests {

        @Test
        fun `uploadResource sends POST to correct endpoint`() {
            mockSuccessResponse()

            val result = subject.uploadResource(
                path = IcmPath.from("icm://path/to/file.jpg"),
                data = byteArrayOf(1, 2, 3),
                production = true,
                update = true,
            )

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            requestSlot.captured.url.toString().shouldBeEqualTo("${evolveConfig.companyUrl}/authoring/api/system/v1/resources")
            requestSlot.captured.method.shouldBeEqualTo("POST")
            requestSlot.captured.header("Authorization").shouldBeEqualTo("Bearer ${evolveConfig.contentAuthorApiKey}")
            multipartPartValueString("name").shouldBeEqualTo("file.jpg")
            multipartPartValueString("path").shouldBeEqualTo("icm://path/to")
            multipartPartValueBool("production").shouldBeEqualTo(true)
            multipartPartValueBool("update").shouldBeEqualTo(true)
            multipartPartValue("dataStream").shouldBeEqualTo(byteArrayOf(1, 2, 3))
        }

        @Test
        fun `uploadResource returns Failure on 400 response`() {
            mockFailureResponse(400)

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
            val failure = result as HttpResult.Failure
            failure.error.status.shouldBeEqualTo(400)
            failure.error.code.shouldBeEqualTo(1001L)
        }

        @Test
        fun `uploadResource returns Exception on IOException`() {
            every { mockCall.execute() } throws IOException("connection refused")

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
            (result as HttpResult.Exception).cause.message?.contains("connection refused").shouldBeEqualTo(true)
        }
    }

    @Nested
    inner class CreateTemplateDraftTests {

        private val jsonData = """{"type":"template"}""".toByteArray()

        @Test
        fun `createTemplateDraft sends POST to correct endpoint with expected fields and returns Success`() {
            mockSuccessResponse("""{"draft":{"guid":"draft-guid","url":"https://ca.example.com/draft"},"result":{"valid":true}}""")

            val result = subject.createTemplateDraft("MyTemplate", IcmPath.from("icm://folder"), IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            val success = result as HttpResult.Success
            success.response.draft.guid.shouldBeEqualTo("draft-guid")
            success.response.result.valid.shouldBeEqualTo(true)
            requestSlot.captured.url.toString().shouldBeEqualTo("${evolveConfig.companyUrl}/authoring/api/system/v1/templateDraft/createFromJson")
            requestSlot.captured.method.shouldBeEqualTo("POST")
            multipartPartNames(requestSlot.captured).containsAll(listOf("name", "baseTemplatePath", "holder", "holderType", "jsonData", "state")).shouldBeEqualTo(true)
            multipartPartValueString("name").shouldBeEqualTo("MyTemplate")
            multipartPartValueString("holder").shouldBeEqualTo(evolveConfig.holder)
            multipartPartValueString("holderType").shouldBeEqualTo(evolveConfig.holderType)
        }

        @Test
        fun `createTemplateDraft returns Failure on 400 response`() {
            mockFailureResponse(400)

            val result = subject.createTemplateDraft("MyTemplate", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
        }

        @Test
        fun `createTemplateDraft returns Exception on IOException`() {
            every { mockCall.execute() } throws IOException("timeout")

            val result = subject.createTemplateDraft("MyTemplate", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
        }
    }

    @Nested
    inner class CreateBlockDraftTests {

        private val jsonData = """{"type":"block"}""".toByteArray()

        @Test
        fun `createBlockDraft sends POST to correct endpoint with expected fields and returns Success`() {
            mockSuccessResponse("""{"draft":{"guid":"block-guid","url":"https://ca.example.com/draft"},"result":{"valid":false,"contentMigrationResult":{"Result":"Warning"}}}""")

            val result = subject.createBlockDraft(
                "MyBlock",
                IcmPath.from("icm://folder"),
                IcmPath.from("icm://base/templ.wfd"),
                jsonData
            )

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            val success = result as HttpResult.Success
            success.response.draft.guid.shouldBeEqualTo("block-guid")
            success.response.result.contentMigrationResult?.result.shouldBeEqualTo(ContentMigrationResultStatus.Warning)
            requestSlot.captured.url.toString().shouldBeEqualTo("${evolveConfig.companyUrl}/authoring/api/system/v1/blockDraft/createFromJson")
            requestSlot.captured.method.shouldBeEqualTo("POST")
            multipartPartValueString("name").shouldBeEqualTo("MyBlock")
            multipartPartValueString("state").shouldBeEqualTo("S_block_scenario_assigned")
        }

        @Test
        fun `createBlockDraft returns Failure on 400 response`() {
            mockFailureResponse(400)

            val result = subject.createBlockDraft("MyBlock", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
        }

        @Test
        fun `createBlockDraft returns Exception on IOException`() {
            every { mockCall.execute() } throws IOException("reset")

            val result = subject.createBlockDraft("MyBlock", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
        }
    }

    @Nested
    inner class CreateRuleDraftTests {

        private val jsonData = """{"type":"rule"}""".toByteArray()

        @Test
        fun `createRuleDraft sends POST to correct endpoint with expected fields and returns Success`() {
            mockSuccessResponse("""{"guid":"rule-guid","url":"https://ca.example.com/rule","path":"/Rules/MyRule.jrd"}""")

            val result = subject.createRuleDraft(
                "MyRule",
                IcmPath.from("icm://folder"),
                IcmPath.from("icm://base/templ.wfd"),
                jsonData
            )

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            val success = result as HttpResult.Success
            success.response.guid.shouldBeEqualTo("rule-guid")
            success.response.path.shouldBeEqualTo("/Rules/MyRule.jrd")
            requestSlot.captured.url.toString().shouldBeEqualTo("${evolveConfig.companyUrl}/authoring/api/system/v1/ruleDraft/createFromJson")
            requestSlot.captured.method.shouldBeEqualTo("POST")
            multipartPartNames(requestSlot.captured).containsAll(listOf("name", "baseTemplatePath", "holder", "holderType", "jsonData", "state")).shouldBeEqualTo(true)
            multipartPartValueString("name").shouldBeEqualTo("MyRule")
            multipartPartValueString("state").shouldBeEqualTo("S_rule_scenario_assigned")
        }

        @Test
        fun `createRuleDraft returns Failure on 400 response`() {
            mockFailureResponse(400)

            val result = subject.createRuleDraft("MyRule", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
            val failure = result as HttpResult.Failure
            failure.error.detail.shouldBeEqualTo("Invalid input")
        }

        @Test
        fun `createRuleDraft returns Exception on IOException`() {
            every { mockCall.execute() } throws IOException("network error")

            val result = subject.createRuleDraft("MyRule", null, IcmPath.from("icm://base/templ.wfd"), jsonData)

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
            (result as HttpResult.Exception).cause.message?.contains("network error").shouldBeEqualTo(true)
        }
    }

    @Nested
    inner class ExecuteActionTests {

        @Test
        fun `executeAction sends POST with JSON body and returns Success`() {
            mockSuccessResponse()

            val result = subject.executeAction("my-action", "my-guid", ObjectType.TemplateDraft)

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            (result as HttpResult.Success).response.shouldBeEqualTo(Unit)
            requestSlot.captured.url.toString().shouldBeEqualTo("${evolveConfig.companyUrl}/authoring/api/system/v1/approvalProcesses/executeAction")
            requestSlot.captured.method.shouldBeEqualTo("POST")
            requestSlot.captured.header("Authorization").shouldBeEqualTo("Bearer ${evolveConfig.contentAuthorApiKey}")
            val contentType = requestSlot.captured.body?.contentType()
            "${contentType?.type}/${contentType?.subtype}".shouldBeEqualTo("application/json")
            val buffer = okio.Buffer()
            requestSlot.captured.body!!.writeTo(buffer)
            buffer.readUtf8().shouldBeEqualTo("""{"actionId":"my-action","guid":"my-guid","objectType":"TemplateDraft"}""")
        }

        @Test
        fun `executeAction returns Failure on 400 response`() {
            mockFailureResponse(400)

            val result = subject.executeAction("publish", "guid-123", ObjectType.BlockDraft)

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
            val failure = result as HttpResult.Failure
            failure.error.status.shouldBeEqualTo(400)
        }

        @Test
        fun `executeAction returns Exception on IOException`() {
            every { mockCall.execute() } throws IOException("socket closed")

            val result = subject.executeAction("publish", "guid-123", ObjectType.BlockDraft)

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
            (result as HttpResult.Exception).cause.message?.contains("socket closed").shouldBeEqualTo(true)
        }


        @Test
        fun `executeAction serializes all ObjectType values correctly`() {
            for (objectType in ObjectType.entries) {
                mockSuccessResponse()

                subject.executeAction("action", "guid", objectType)

                val buffer = okio.Buffer()
                requestSlot.captured.body!!.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                bodyString.contains("\"objectType\":\"${objectType.name}\"").shouldBeEqualTo(true)
            }
        }
    }

    @Nested
    inner class RetryTests {
        private val errorBody = """{"status":503,"title":"Service Unavailable","code":0,"detail":""}"""

        @Test
        fun `retries on 5xx and returns Success on next attempt`() {
            every { mockCall.execute() } returnsMany listOf(buildResponse(503, errorBody), buildResponse(200))

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            verify(exactly = 2) { mockCall.execute() }
        }

        @Test
        fun `exhausts retries on repeated 5xx and returns last Failure`() {
            every { mockCall.execute() } returnsMany listOf(
                buildResponse(503, errorBody),
                buildResponse(503, errorBody),
                buildResponse(503, errorBody),
            )

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
            verify(exactly = 3) { mockCall.execute() }
        }

        @Test
        fun `does not retry on 4xx`() {
            every { mockCall.execute() } returns buildResponse(400, """{"status":400,"title":"Bad Request","code":1001,"detail":"Invalid input"}""")

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Failure<*, *>>()
            verify(exactly = 1) { mockCall.execute() }
        }

        @Test
        fun `retries on IOException and returns Success on next attempt`() {
            var attempt = 0
            every { mockCall.execute() } answers {
                if (attempt++ == 0) throw IOException("timeout") else buildResponse(200)
            }

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Success<*, *>>()
            verify(exactly = 2) { mockCall.execute() }
        }

        @Test
        fun `exhausts retries on repeated IOException and returns last Exception`() {
            every { mockCall.execute() } throws IOException("timeout")

            val result = subject.uploadResource(IcmPath.from("icm://path/to/file.jpg"), byteArrayOf())

            result.shouldBeOfInstance<HttpResult.Exception<*, *>>()
            (result as HttpResult.Exception).cause.message?.contains("timeout").shouldBeEqualTo(true)
            verify(exactly = 3) { mockCall.execute() }
        }

        private fun buildResponse(code: Int, body: String = "") = Response.Builder()
            .request(Request.Builder().url("https://ca.example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code < 400) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun mockSuccessResponse(body: String = ""): Call {
        val response = Response.Builder()
            .request(Request.Builder().url("https://ca.example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns response
        return mockCall
    }

    private fun mockFailureResponse(code: Int = 400, body: String = """{"status":400,"title":"Bad Request","code":1001,"detail":"Invalid input"}"""): Call {
        val response = Response.Builder()
            .request(Request.Builder().url("https://ca.example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Bad Request")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns response
        return mockCall
    }

    private fun multipartPartNames(request: Request): List<String> {
        val body = request.body as MultipartBody
        return body.parts.mapNotNull { part ->
            part.headers?.get("Content-Disposition")
                ?.split(";")
                ?.firstNotNullOfOrNull { segment ->
                    val trimmed = segment.trim()
                    if (trimmed.startsWith("name=")) trimmed.removePrefix("name=").trim('"') else null
                }
        }
    }

    private fun multipartPartValueBool(partName: String): Boolean? {
        return multipartPartValue(partName)?.let { String(it).toBoolean() }
    }

    private fun multipartPartValueString(partName: String): String? {
        return multipartPartValue(partName)?.let(::String)
    }

    private fun multipartPartValue(partName: String): ByteArray? {
        val body = requestSlot.captured.body as MultipartBody
        return body.parts.firstNotNullOfOrNull { part ->
            val disposition = part.headers?.get("Content-Disposition") ?: return@firstNotNullOfOrNull null
            val name = disposition.split(";").firstNotNullOfOrNull { segment ->
                val trimmed = segment.trim()
                if (trimmed.startsWith("name=")) trimmed.removePrefix("name=").trim('"') else null
            }
            if (name == partName) {
                val buffer = okio.Buffer()
                part.body.writeTo(buffer)
                buffer.readByteArray()
            } else null
        }
    }
}

