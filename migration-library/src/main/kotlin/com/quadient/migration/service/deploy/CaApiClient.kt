package com.quadient.migration.service.deploy

import com.quadient.migration.api.MigConfig
import com.quadient.migration.shared.IcmPath
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(CaApiClient::class.java)

class CaApiClient(private val migConfig: MigConfig, private val httpClient: OkHttpClient) {
    private val evolveConfig by lazy {
        requireNotNull(migConfig.inspireConfig.evolveConfig) {
            "migrationConfig.evolveConfig must be set to use Evolve inspireOutput"
        }
    }
    private val baseUrl by lazy { evolveConfig.contentAuthorUrl.removeSuffix("/") }
    private val json = Json { ignoreUnknownKeys = true }

    fun uploadResource(
        path: IcmPath,
        data: ByteArray,
        production: Boolean = true,
        update: Boolean = false,
    ): HttpResult<Unit, ApiBadRequestException> {
        logger.debug("Uploading resource to {} (production={}, update={})", path, production, update)
        val filename = path.filename()
        return createRequest("/resources")
            .postMultipartForm {
                addFormDataPart("name", filename)
                addFormDataPart("path", path.parentDir().toString())
                addFormDataPart("production", production.toString())
                addFormDataPart("update", update.toString())
                addFormDataPart("dataStream", filename, data.toRequestBody())
            }
            .executeRetrying()
    }

    fun createTemplateDraft(
        name: String,
        targetFolder: IcmPath?,
        baseTemplatePath: IcmPath,
        data: ByteArray,
    ): HttpResult<DraftJsonIpsResult, ApiBadRequestException> {
        logger.debug("Creating template draft to {}/{}.jld", targetFolder, name)
        return createRequest("/templateDraft/createFromJson")
            .postMultipartForm {
                addFormDataPart("name", name)
                addFormDataPart("baseTemplatePath", baseTemplatePath.toString())
                addFormDataPart("holder", evolveConfig.holder)
                addFormDataPart("holderType", evolveConfig.holderType)
                addFormDataPart("jsonData", null, data.toRequestBody())
                addFormDataPart("state", "S_template_scenario_assigned")
                addNonEmptyFormDataPart("folder", targetFolder?.toString())
            }
            .executeRetrying()
    }

    fun createBlockDraft(
        name: String,
        targetFolder: IcmPath?,
        baseTemplatePath: IcmPath,
        data: ByteArray,
    ): HttpResult<DraftJsonIpsResult, ApiBadRequestException> {
        logger.debug("Creating block draft to {}/{}.jld", targetFolder, name)
        return createRequest("/blockDraft/createFromJson")
            .postMultipartForm {
                addFormDataPart("baseTemplatePath", baseTemplatePath.toString())
                addFormDataPart("holder", evolveConfig.holder)
                addFormDataPart("holderType", evolveConfig.holderType)
                addFormDataPart("jsonData", null, data.toRequestBody())
                addFormDataPart("name", name)
                addFormDataPart("state", "S_block_scenario_assigned")
                addNonEmptyFormDataPart("folder", targetFolder?.toString())
            }
            .executeRetrying()
    }

    fun createRuleDraft(
        name: String,
        targetFolder: IcmPath?,
        baseTemplatePath: IcmPath,
        data: ByteArray,
    ): HttpResult<CreateDraftWithVFFResult, ApiBadRequestException> {
        logger.debug("Creating rule draft to {}/{}.jrd", targetFolder, name)
        return createRequest("/ruleDraft/createFromJson")
            .postMultipartForm {
                addFormDataPart("baseTemplatePath", baseTemplatePath.toString())
                addFormDataPart("holder", evolveConfig.holder)
                addFormDataPart("holderType", evolveConfig.holderType)
                addFormDataPart("jsonData", null, data.toRequestBody())
                addFormDataPart("name", name)
                addFormDataPart("state", "S_rule_scenario_assigned")
                addNonEmptyFormDataPart("folder", targetFolder?.toString())
            }
            .executeRetrying()
    }

    fun executeAction(actionId: String, guid: String, objectType: ObjectType): HttpResult<Unit, ApiBadRequestException> {
        val body = json.encodeToString(ExecuteActionRequest(actionId = actionId, guid = guid, objectType = objectType.toString()))

        return createRequest("/approvalProcesses/executeAction")
            .post(body.toRequestBody("application/json".toMediaType()))
            .executeRetrying()
    }

    private fun MultipartBody.Builder.addNonEmptyFormDataPart(name: String, value: String?) {
        if (value != null) {
            addFormDataPart(name, value)
        }
    }

    private fun Request.Builder.postMultipartForm(block: MultipartBody.Builder.() -> Unit): Request.Builder {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        block(builder)
        return this.post(builder.build())
    }

    private inline fun <reified T, reified E> Request.Builder.executeRetrying(
        maxRetries: Int = 3,
        delayMs: Long = evolveConfig.apiRetryDelayMs
    ): HttpResult<T, E> {
        return httpClient.newCall(this.build()).executeRetrying(maxRetries, delayMs)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T, reified E> Call.executeRetrying(maxRetries: Int = 3, delayMs: Long): HttpResult<T, E> {
        var lastError: HttpResult<T, E>? = null
        repeat(maxRetries) { attempt ->
            val call = if (attempt == 0) this else this.clone()
            try {
                logger.debug("Executing request ({}/{}) {}", attempt + 1, maxRetries, call.request())
                call.execute().use { response ->
                    if (response.isSuccessful) {
                        return if (T::class == Unit::class) {
                            HttpResult.Success(Unit as T)
                        } else {
                            HttpResult.Success(json.decodeFromStream<T>(response.body.byteStream()))
                        }
                    }

                    try {
                        val body = response.body.string()
                        logger.error("Attempt {}/{} failed: {} {} - {}", attempt + 1, maxRetries, response.code, response.message, body)
                        val jsonBody = json.decodeFromString<E>(body)
                        lastError = HttpResult.Failure(jsonBody)
                        if (!response.code.isRetryable()) return HttpResult.Failure(jsonBody)
                    } catch (e: Exception) {
                        logger.error("Error while parsing response: {} {} {}", response.code, response.message, e.toString())
                        lastError = HttpResult.Exception(e)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Attempt {}/{} failed: {}", attempt + 1, maxRetries, e.message)
                lastError = HttpResult.Exception(e)
            }
            if (attempt < maxRetries - 1) Thread.sleep(delayMs)
        }

        return lastError ?: HttpResult.Exception(RuntimeException("HTTP call failed after $maxRetries attempts"))
    }

    private fun Int.isRetryable() = this in 500..599 || this == 429

    private fun createRequest(url: String): Request.Builder {
        return Request.Builder().url("$baseUrl$url").header("Authorization", "Bearer ${evolveConfig.contentAuthorApiKey}")
    }
}

sealed interface HttpResult<T, E> {
    data class Success<T, E>(val response: T) : HttpResult<T, E>
    data class Failure<T, E>(val error: E) : HttpResult<T, E>
    data class Exception<T, E>(val cause: Throwable) : HttpResult<T, E>
}

@Serializable
enum class ObjectType {
    BlockDraft,
    TemplateDraft,
    RuleDraft,
    ResourceDraft,
    DataDefinitionDraft,
    FunctionDraft,
    BaseTemplateDraft,
    FormTemplateDraft,
    VisualComponentDraft,
    DigitalCompositionDraft,
    ChangeSetDraft,
    CompanyStyleDraft,
    SnippetDraft
}

@Serializable
data class ApiBadRequestException(
    val status: Int = 0,
    val title: String = "",
    val code: Long = 0,
    val detail: String = "",
)

@Serializable
data class DraftJsonIpsResult(val draft: CreateDraftResult, val result: CreateIcmObjectResult)

@Serializable
data class CreateDraftResult(val guid: String, val url: String)

@Serializable
data class CreateIcmObjectResult(
    val contentMigrationResult: ContentMigrationResult? = null,
    val valid: Boolean = false,
)

@Serializable
data class ContentMigrationResult(
    @SerialName("Result")
    val result: ContentMigrationResultStatus = ContentMigrationResultStatus.Ok,
    val details: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

@Serializable
enum class ContentMigrationResultStatus { Ok, Warning, Error, FatalError }

@Serializable
data class CreateDraftWithVFFResult(
    val guid: String,
    val path: String? = null,
    val url: String,
)

@Serializable
data class ExecuteActionRequest(
    val actionId: String,
    val guid: String,
    val objectType: String,
    val message: String? = null,
    val userOrGroupName: String? = null,
    val operationsData: Map<String, Map<String, String>>? = null,
)
