package com.quadient.migration.service.ipsclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.IcmClient
import com.quadient.migration.api.IcmFileMetadata
import com.quadient.migration.api.IpsConfig
import com.quadient.migration.service.inspirebuilder.FontKey
import com.quadient.migration.tools.surroundWith
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.*
import kotlin.time.Duration.Companion.seconds

class IpsService(private val config: IpsConfig) : Closeable, IcmClient {
    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }

    private val _client = lazy { IpsClient(config.host, config.port, config.timeoutSeconds.seconds) }
    val client: IpsClient by _client

    private val logger = LoggerFactory.getLogger(IpsService::class.java)!!
    private val uploadedResources: MutableMap<String, UploadedFile> = mutableMapOf()

    fun xml2wfd(wfdXml: String, outputPath: String): OperationResult {
        val wfdXmlIpsLocation = "memory://${UUID.randomUUID()}"

        client.upload(wfdXmlIpsLocation, wfdXml.toByteArray()).ifNotSuccess {
            val message = "Failed to upload wfdXml input for conversion, $wfdXmlIpsLocation, $it"
            logger.error(message)
            return OperationResult.Failure(message)
        }

        val result = client.xml2wfd(wfdXmlIpsLocation, outputPath)
        val operationResult = result.waitAndAckJobOrLogError()

        client.remove(wfdXmlIpsLocation).ifNotSuccess {
            logger.error("Failed to cleanup wfdXml input memory: {}", it)
        }

        return operationResult
    }

    fun wfd2xml(wfdPath: String): String {
        val resultLocation = "memory://${UUID.randomUUID()}"

        val result = client.wfd2xml(wfdPath, resultLocation)
        val operationResult = result.waitAndAckJobOrLogError()
        if (operationResult is OperationResult.Failure) {
            throw IpsClientException(operationResult.message)
        }

        val outputResult = client.download(resultLocation)
        if (outputResult !is IpsResult.Ok) {
            throw IpsClientException("Failed to download result xml: '$outputResult'")
        }

        client.remove(resultLocation).ifNotSuccess {
            logger.error("Failed to cleanup wfdXml output memory: {}", it)
        }

        return String(outputResult.customData).trimIndent()
    }

    fun deployJld(
        baseTemplate: String, type: String, moduleName: String, xmlContent: String, outputPath: String
    ): OperationResult {
        logger.debug("Starting deployment of $outputPath.")

        val xmlContentIpsLocation = "memory://${UUID.randomUUID()}"
        val commandXmlIpsLocation = "memory://${UUID.randomUUID()}"

        val openResult = client.open(baseTemplate)
        if (openResult is IpsResult.Ok) {
            try {
                client.waitForJob(openResult.jobId).ifNotSuccess {
                    val message = "Waiting for workflow to open failed, $openResult.jobId"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }

                val command = Coms(Coms.ImportLayoutXml(moduleName, xmlContentIpsLocation))
                val commandXml = xmlMapper.writeValueAsString(command)
                logger.debug(commandXml)

                client.upload(commandXmlIpsLocation, commandXml.toByteArray()).ifNotSuccess {
                    val message = "Failed to upload command xml, $commandXmlIpsLocation, $it"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }
                client.upload(xmlContentIpsLocation, xmlContent.toByteArray()).ifNotSuccess {
                    val message = "Failed to upload content xml, $xmlContentIpsLocation, $it"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }

                client.editWfd(openResult.workFlowId, commandXmlIpsLocation).ifNotSuccess {
                    val message = "Failed to edit wfd with content xml to create $outputPath. $it"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }

                val result = client.extractJld(openResult.workFlowId, outputPath, type)
                return result.waitAndAckJobOrLogError()
            } finally {
                client.close(openResult.workFlowId).ifOk { closeResult ->
                    closeResult.waitAndAckJobOrLogError()
                }
                client.ackJob(openResult.jobId).ifNotSuccess {
                    logger.error("Failed to ack open workflow: $it ")
                }
                client.remove(commandXmlIpsLocation).ifNotSuccess {
                    logger.error("Failed to cleanup command.xml memory: {}", it)
                }
                client.remove(xmlContentIpsLocation).ifNotSuccess {
                    logger.error("Failed to cleanup content xml memory: {}", it)
                }
            }
        }

        return OperationResult.Failure(openResult.toString())
    }

    fun setProductionApprovalState(paths: List<String>): OperationResult {
        val pathsLocation = "memory://${UUID.randomUUID()}"
        val json = """{"paths":[${paths.joinToString(",") { it.surroundWith("\"") }}]}"""
        try {
            logger.debug(json)
            client.upload(pathsLocation, json.toByteArray()).ifNotSuccess {
                val message = "Failed to upload paths.json $pathsLocation, $it"
                logger.error(message)
                return OperationResult.Failure(message)
            }
            return runWfd("setApprovalState.wfd", listOf("-difJSONDataInput", pathsLocation))
        } finally {
            client.remove(pathsLocation).ifNotSuccess {
                logger.error("Failed to cleanup paths.json memory: {}", it)
            }
        }
    }

    fun gatherFontData(fontRootFolder: String): String {
        val resultLocation = "memory://${UUID.randomUUID()}"

        val result =
            runWfd("gatherFontData.wfd", listOf("-f", resultLocation, "-fontRootFolderFontRootFolder", fontRootFolder))
        if (result !is OperationResult.Success) {
            throw IpsClientException("Failed to gather font data from root folder: $fontRootFolder")
        }

        val resultXml = client.download(resultLocation).throwIfNotOk()
        val resultXmlTree = xmlMapper.readTree(String(resultXml.customData))
        return resultXmlTree["fontData"].textValue()
    }

    fun runWfd(wfdPath: String, args: List<String>): OperationResult {
        val uploadedResourcePath = getOrInitCachedWorkflow(wfdPath).getOrElse {
            val message = "Failed to upload file: '$wfdPath'. Error: $it"
            logger.error(message)
            return OperationResult.Failure(message)
        }

        val openResult = client.open(uploadedResourcePath)
        if (openResult is IpsResult.Ok) {
            try {
                client.waitForJob(openResult.jobId).ifNotSuccess {
                    val message = "Waiting for workflow to open failed, $openResult.jobId"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }
                return client.run(openResult.workFlowId.toString(), args).waitAndAckJobOrLogError()
            } finally {
                client.ackJob(openResult.jobId).ifNotSuccess {
                    logger.error("Failed to ack open workflow: $it ")
                }
                client.close(openResult.workFlowId).ifOk { closeResult ->
                    closeResult.waitAndAckJobOrLogError()
                }.ifNotSuccess {
                    logger.error("Failed to close workflow: $it")
                }
            }
        }

        return OperationResult.Failure(openResult.toString())
    }

    private fun getOrInitCachedWorkflow(wfdPath: String): Result<String> {
        val cached = uploadedResources[wfdPath]
        if (cached != null) {
            return Result.success(cached.path)
        }

        val dataStream = this.javaClass.getResource("/$wfdPath")
            ?: return Result.failure(MissingWfdException("Wfd not found: '${wfdPath}'"))

        val uploadDest = "memory://${UUID.randomUUID()}"
        val uploadResult = client.upload(uploadDest, dataStream.readBytes())
        when (uploadResult) {
            is IpsResult.Error -> return Result.failure(Exception("Failed to upload file: $uploadResult"))
            is IpsResult.Exception -> return Result.failure(uploadResult.throwable)
            is IpsResult.Ok -> {}
        }

        uploadedResources[wfdPath] = UploadedFile(uploadDest, {
            val removeResult = client.remove(uploadDest)
            if (removeResult.isNotOk()) {
                logger.error("Failed to cleanup resource '{}'. Operation result: '{}'", wfdPath, removeResult)
            }
        })

        return Result.success(uploadDest)
    }

    private fun waitAndAckAndGetProgressStatus(jobId: JobId): String? {
        client.waitForJob(jobId).ifNotSuccess {
            logger.error("Waiting for job run to finish failed, {}", jobId)
        }
        val log = client.queryJobMessages(jobId)
        logger.debug(log)

        val job = client.queryJobStatus(jobId)

        client.ackJob(jobId).ifNotSuccess {
            logger.error("Failed to ack run workflow: $it ")
        }

        job.ifOk {
            val potentialProgressStatusPart = it.parts[6]
            return if (potentialProgressStatusPart.startsWith("E")) {
                null
            } else {
                potentialProgressStatusPart
            }
        }

        return null
    }

    private fun <W, C> IpsResult<JobId, W, C>.waitAndAckJobOrLogError(): OperationResult {
        return when (this) {
            is IpsResult.Ok -> {
                val progressStatus = waitAndAckAndGetProgressStatus(this.jobId)
                if (progressStatus?.startsWith("Aborted") == true) {
                    val message = "Job finished with aborted progress status: '$progressStatus'"
                    logger.error(message)
                    return OperationResult.Failure(message)
                }
                OperationResult.Success
            }

            is IpsResult.Error -> {
                waitAndAckAndGetProgressStatus(this.jobId)
                val message = "Job failed. '$this'"
                logger.error(message)
                OperationResult.Failure(message)
            }

            is IpsResult.Exception -> {
                val message = "Job failed with unexpected error. Error: '$this'"
                logger.error(message)
                OperationResult.Failure(message)
            }
        }
    }

    override fun close() {
        logger.debug("Cleaning resources for IPS service")
        for (resource in uploadedResources.values) {
            logger.debug("Cleaning up {}", resource)
            resource.onClose()
        }

        if (_client.isInitialized()) {
            client.close()
        }
    }

    fun tryUpload(path: String, data: ByteArray): OperationResult {
        try {
            this.upload(path, data)
            return OperationResult.Success
        } catch (e: Exception) {
            logger.error("Upload Failed.", e)
            return OperationResult.Failure("Upload Failed. ${e.message}")
        }
    }

    override fun upload(path: String, data: ByteArray) {
        require(path.startsWith("icm://")) { "Expected path to start with icm:// but got '$path" }

        client.upload(path, data).throwIfNotOk()
    }

    override fun download(path: String): ByteArray {
        require(path.startsWith("icm://")) { "Expected path to start with icm:// but got '$path" }

        return client.download(path).throwIfNotOk().customData
    }

    override fun approveFiles(paths: List<String>) {
        val result = setProductionApprovalState(paths)
        if (result !is OperationResult.Success) {
            throw IpsClientException("Failed to approve files: $paths")
        }
    }

    override fun fileExists(path: String): Boolean {
        require(path.startsWith("icm://")) { "Expected path to start with icm:// but got '$path" }
        return filesExist(listOf(path)).first()
    }

    override fun filesExist(paths: List<String>): List<Boolean> {
        require(paths.all { it.startsWith("icm://") }) { "Expected all paths to start with icm:// but got '$paths" }
        val pathsLocation = "memory://${UUID.randomUUID()}"
        val resultLocation = "memory://${UUID.randomUUID()}"
        val json = """{"paths":[${paths.joinToString(",") { it.surroundWith("\"") }}]}"""
        try {
            logger.debug(json)
            client.upload(pathsLocation, json.toByteArray()).throwIfNotOk()
            val result = runWfd("fileExists.wfd", listOf("-difJSONDataInput", pathsLocation, "-f", resultLocation))
            if (result !is OperationResult.Success) {
                throw IpsClientException("Failed to check file existence: $paths")
            }
            val resultCsv = client.download(resultLocation).throwIfNotOk()
            val resultLines = String(resultCsv.customData).lines()
            return paths.mapIndexed { index, path ->
                val split = resultLines[index + 1].split(",") // +1 to skip CSV header
                val filePath = split[0].removeSurrounding("\"")
                val fileExists = when (split[1].removeSurrounding("\"")) {
                    "1" -> true
                    "0" -> false
                    else -> throw IllegalStateException("Unexpected result value: ${split[1]}")
                }
                if (filePath != path) {
                    throw IllegalStateException("Unexpected result file path: $filePath")
                }
                fileExists
            }
        } finally {
            client.remove(pathsLocation).ifNotSuccess {
                logger.error("Failed to cleanup paths.json memory: {}", it)
            }
            client.remove(resultLocation).ifNotSuccess {
                logger.error("Failed to cleanup fileExists.wfd result csv memory: {}", it)
            }
        }
    }

    override fun delete(path: String): Boolean {
        require(path.startsWith("icm://")) { "Expected path to start with icm:// but got '$path" }
        return delete(listOf(path)).first()
    }

    override fun delete(paths: List<String>): List<Boolean> {
        require(paths.all { it.startsWith("icm://") }) { "Expected all paths to start with icm:// but got '$paths" }
        val pathsLocation = "memory://${UUID.randomUUID()}"
        val resultLocation = "memory://${UUID.randomUUID()}"
        val json = """{"paths":[${paths.joinToString(",") { it.surroundWith("\"") }}]}"""
        try {
            logger.debug(json)
            client.upload(pathsLocation, json.toByteArray()).throwIfNotOk()
            val result = runWfd("deleteFiles.wfd", listOf("-difJSONDataInput", pathsLocation, "-f", resultLocation))
            if (result != OperationResult.Success) {
                throw IpsClientException("Failed to delete files: $paths")
            }
            val resultCsv = client.download(resultLocation).throwIfNotOk()
            val resultLines = String(resultCsv.customData).lines()
            return paths.mapIndexed { index, path ->
                val split = resultLines[index + 1].split(",") // +1 to skip CSV header
                val filePath = split[0].removeSurrounding("\"")
                val fileExists = when (split[1].removeSurrounding("\"")) {
                    "1" -> true
                    "0" -> false
                    else -> throw IllegalStateException("Unexpected result value: ${split[1]}")
                }
                if (filePath != path) {
                    throw IllegalStateException("Unexpected result file path: $filePath")
                }
                fileExists
            }
        } finally {
            client.remove(pathsLocation).ifNotSuccess {
                logger.error("Failed to cleanup paths.json memory: {}", it)
            }
            client.remove(resultLocation).ifNotSuccess {
                logger.error("Failed to cleanup deleteFiles.wfd result csv memory: {}", it)
            }
        }
    }

    override fun readMetadata(path: String): IcmFileMetadata {
        require(path.startsWith("icm://")) { "Expected path to start with icm:// but got '$path" }
        return readMetadata(listOf(path)).first()
    }

    override fun readMetadata(paths: List<String>): List<IcmFileMetadata> {
        require(paths.all { it.startsWith("icm://") }) { "Expected all paths to start with icm:// but got '$paths" }
        val pathsLocation = "memory://${UUID.randomUUID()}"
        val resultLocation = "memory://${UUID.randomUUID()}"
        val json = """{"paths":[${paths.joinToString(",") { it.surroundWith("\"") }}]}"""
        try {
            logger.debug(json)
            client.upload(pathsLocation, json.toByteArray()).throwIfNotOk()
            val result = runWfd("readMetadata.wfd", listOf("-difJSONDataInput", pathsLocation, "-f", resultLocation))
            if (result != OperationResult.Success) {
                throw IpsClientException("Failed to delete files: $paths")
            }
            val resultJson = client.download(resultLocation).throwIfNotOk()

            val mapper = ObjectMapper()
            val result2 = mapper.readTree(String(resultJson.customData))
            return result2.get("paths").map {
                val text = it.get("metadata").textValue()
                val metadata = mapper.readValue(text, Metadata::class.java)
                IcmFileMetadata(
                    path = it.get("path").textValue(),
                    system = metadata.system.toMutableMap(),
                    user = metadata.user.toMutableMap(),
                )
            }
        } finally {
            client.remove(pathsLocation).ifNotSuccess {
                logger.error("Failed to cleanup paths.json memory: {}", it)
            }
            client.remove(resultLocation).ifNotSuccess {
                logger.error("Failed to cleanup deleteFiles.wfd result csv memory: {}", it)
            }
        }
    }
}

@Serializable
data class Metadata @JsonCreator constructor(
    @JsonProperty("system") val system: Map<String, List<String>>,
    @JsonProperty("user") val user: Map<String, List<String>>
)
data class UploadedFile(val path: String, val onClose: () -> Unit)
sealed interface OperationResult {
    object Success : OperationResult
    data class Failure(val message: String) : OperationResult
}

class MissingWfdException(message: String) : Exception(message)
