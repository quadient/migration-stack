package com.quadient.migration.service.ipsclient

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import kotlin.time.Duration

class IpsClient(private val host: String, private val port: Int, private val timeout: Duration) : Closeable {
    private val _connection = lazy { IpsConnection() }
    private val connection: IpsConnection by _connection
    private val logger = LoggerFactory.getLogger(IpsClient::class.java)!!

    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }

    fun ackJob(jobId: JobId): IpsResult<Unit, Unit, Unit> {
        val command = "ackj $jobId"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun run(wfdPath: String, args: List<String>): IpsResult<JobId, Unit, Unit> {
        val command = "run $wfdPath${args.joinToString(prefix = " ", separator = " ")}"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun runw(wfdPath: String, args: List<String>): IpsResult<JobId, Unit, Unit> {
        val command = "runw $wfdPath${args.joinToString(prefix = " ", separator = " ")}"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun ping(): IpsResult<Unit, Unit, Unit> {
        val command = "ping"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun open(path: String): IpsResult<JobId, WorkFlowId, Unit> {
        val command = "open $path"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun close(workflowId: WorkFlowId): IpsResult<JobId, WorkFlowId, Unit> {
        val command = "close $workflowId"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun upload(path: String, bytes: ByteArray): IpsResult<Unit, Unit, Unit> {
        try {
            val command = "upload $path;${bytes.count()}"
            connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }

            var line = connection.readLine()
            if (line != "ok;upload-start") {
                return IpsClientException("Failed to upload file. Expected upload start, got '$line'").toIpsResult()
            }
            connection.socket.outputStream.write(bytes)

            line = connection.readLine()
            if (line != "ok;upload-finish") {
                return IpsClientException("Failed to upload file. Expected upload finish, got '$line'").toIpsResult()
            }

            return IpsResult.Ok(IpsResponse("ok;"))
        } catch (e: Exception) {
            return IpsClientException("Failed to write bytes to ips", e).toIpsResult()
        }
    }

    fun download(path: String): IpsResult<Unit, Unit, ByteArray> {
        try {
            val command = "download $path;"
            connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }

            val response = connection.readLine().split(";")
            if (response.getOrNull(0) != "ok" || response.getOrNull(1)?.toLongOrNull() == null) {
                return IpsClientException("Failed to download file. Expected byte size, got $response").toIpsResult()
            }
            val bytesToRead = response[1].toInt()
            val bytes = ByteArray(bytesToRead)
            connection.reader.readNBytes(bytes, 0, bytesToRead)

            val line = connection.readLine()
            if (line != "ok;download-finish") {
                return IpsClientException("Failed to upload file. Expected download finish, got '$line'").toIpsResult()
            }

            return IpsResult.Ok(IpsResponse("ok;"), bytes)
        } catch (e: Exception) {
            return IpsClientException("Failed to read bytes from ips", e).toIpsResult()
        }
    }

    fun remove(path: String): IpsResult<Unit, Unit, Unit> {
        val command = "remove $path;"
        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun waitForJob(
        jobIdValue: Int, timeoutSeconds: Int = this.timeout.inWholeSeconds.toInt()
    ) = waitForJob(JobId(jobIdValue), timeoutSeconds)

    fun waitForJob(
        jobId: JobId,
        timeoutSeconds: Int = this.timeout.inWholeSeconds.toInt()
    ): IpsResult<Unit, Unit, WaitForJobResult> {
        val command = "wfj $jobId $timeoutSeconds;"
        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        val response = connection.readLineWithTimeout(timeoutSeconds)
        val status = when (response) {
            "ok;finished" -> WaitForJobResult.Finished
            "ok;expired" -> WaitForJobResult.Expired
            else -> return IpsClientException("Failed to read response from ips, invalid status: $response").toIpsResult()
        }

        return IpsResult.Ok(IpsResponse(response), status)
    }


    fun editWfd(workflowId: WorkFlowId, commandPath: String): IpsResult<Unit, Unit, Unit> {
        val ipsMemLocation = "memory://${UUID.randomUUID()}"

        val command = "editf $workflowId $commandPath;${ipsMemLocation}"

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }

        val result: IpsResult<Unit, Unit, Unit> = connection.readResponse()
        if (result !is IpsResult.Ok) {
            return result
        }

        val outputResult = download(ipsMemLocation)
        if (outputResult !is IpsResult.Ok) {
            return IpsClientException("Failed to download output.xml: '$outputResult'").toIpsResult()
        }
        val output: Output = xmlMapper.readValue(String(outputResult.customData).trimIndent(), Output::class.java)

        val errors = output.values?.errors
        if (errors != null) {
            return IpsClientException("Output.xml contains errors: '$errors'").toIpsResult()
        }

        val removeResult = remove(ipsMemLocation)
        if (removeResult !is IpsResult.Ok) {
            return removeResult
        }

        return result
    }

    fun xml2wfd(inputPath: String, outputPath: String): IpsResult<JobId, Unit, Unit> {
        val command = """xml2wfd "$inputPath";"$outputPath""""

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun wfd2xml(inputPath: String, outputPath: String): IpsResult<JobId, Unit, Unit> {
        val command = """wfd2xml "$inputPath";"$outputPath"; -exportusedfiles false"""

        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun extractJld(workflowId: WorkFlowId, outputPath: String, type: String): IpsResult<JobId, Unit, Unit> {
        val command =
            """run $workflowId -e json -configDocumentLayout "eyJGaW5hbGl6ZURpc2Nvbm5lY3RlZFZhcmlhYmxlc0FzUmVmZXJlbmNlIjp0cnVlLCJEZXRlY3RMYW5ndWFnZXNGcm9tQ29udGVudCI6dHJ1ZX0=" -type $type -languageDocumentLayout en_us -f "$outputPath""""
        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun extractJldStyleDefinition(workflowId: WorkFlowId, outputPath: String): IpsResult<JobId, Unit, Unit> {
        val command =
            """run $workflowId -e json -type resources -f "$outputPath""""
        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    fun queryJobMessages(jobId: JobId): String {
        val command = "qjm $jobId"
        connection.writeLine(command).getOrThrow()
        val bytesToRead = connection.readLine().split(";")[1].toInt()
        val buf = ByteArray(bytesToRead)
        connection.reader.read(buf, 0, bytesToRead)
        return String(buf)
    }

    fun queryJobStatus(jobId: JobId): IpsResult<JobId, WorkFlowId, Unit> {
        val command = "qj $jobId"
        connection.writeLine(command).getOrElse { return IpsFailedWriteException(command, it).toIpsResult() }
        return connection.readResponse()
    }

    override fun close() {
        if (_connection.isInitialized()) {
            connection.close()
        }
    }

    private inner class IpsConnection : Closeable {
        val socket = Socket()
        val reader: BufferedInputStream
        val writer: OutputStreamWriter

        init {
            try {
                socket.connect(InetSocketAddress(host, port), timeout.inWholeMilliseconds.toInt())
                writer = socket.outputStream.writer()
                reader = socket.inputStream.buffered()
                // +1 because we supply timeout to IPS as is, and we might start waiting before the IPS server
                // starts thus time outing earlier than expected
                socket.soTimeout = (timeout.inWholeSeconds.toInt() + 1) * 1000
            } catch (e: Exception) {
                throw IpsClientException("Failed to connect to $host:$port", e)
            }
        }

        fun readLine(): String {
            val buf = StringBuilder()

            var c = reader.read()
            while (c != -1) {
                val char = c.toChar()

                if (char == '\r') {
                    val c2 = reader.read()
                    if (c2 == -1) {
                        break
                    }

                    val char2 = c2.toChar()
                    if (char2 == '\n') {
                        break
                    } else {
                        buf.append(char2)
                    }
                }

                buf.append(char)
                c = reader.read()
            }

            val line = buf.toString()
            logger.trace("Read line: $line")
            return line
        }

        fun readLineWithTimeout(timeoutSeconds: Int): String {
            val originalTimeout = socket.soTimeout
            try {
                socket.soTimeout = (timeoutSeconds + 1) * 1000
                return readLine()
            } finally {
                socket.soTimeout = originalTimeout
            }
        }

        fun <JId, WfId, Custom> readResponse(): IpsResult<JId, WfId, Custom> {
            return try {
                val response = IpsResponse(readLine())
                return response.toIpsResult()
            } catch (e: Exception) {
                IpsClientException("Failed to read response", e).toIpsResult()
            }
        }

        fun write(message: String) = writer.runCatching {
            write(message)
            flush()
        }

        fun writeLine(message: String) = writer.runCatching {
            logger.trace("Executing ips command: {}", message)
            write(message)
            write("\n")
            flush()
        }

        override fun close() {
            reader.close()
            writer.close()
            socket.close()
        }
    }
}

@JacksonXmlRootElement(localName = "Output")
class Output {
    @JacksonXmlProperty(localName = "Values")
    var values: Values? = null

    class Values {
        @JacksonXmlProperty(localName = "Errors")
        var errors: Errors? = null
    }

    class Errors {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Error")
        var errorList: List<ErrorEntry>? = null

        override fun toString() = "[${errorList?.joinToString(",") { it.toString() }}]"
    }

    class ErrorEntry {
        @JacksonXmlProperty(isAttribute = true, localName = "Code")
        var code: String? = null

        @JacksonXmlProperty(isAttribute = true, localName = "Severity")
        var severity: String? = null

        @JacksonXmlText
        var message: String? = null

        override fun toString() = "(code=$code,severity=$severity,message=$message)"
    }
}

@Serializable
data class Coms(
    @JacksonXmlProperty(localName = "ImportLayoutXml") val values: ImportLayoutXml
) {
    @Serializable
    data class ImportLayoutXml(
        @JacksonXmlProperty(localName = "ModuleName") val moduleName: String,

        @JacksonXmlProperty(localName = "FileName") val fileName: String
    )
}
