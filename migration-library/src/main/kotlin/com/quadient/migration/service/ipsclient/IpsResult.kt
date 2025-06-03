package com.quadient.migration.service.ipsclient

enum class IpsResponseStatus { Ok, Error }

// Allows us to avoid null checks in the rest of the code
// Generics should be set and encapsulated properly inside IpsClient
// and thus eliminate the risk of misuse. If any of this is set wrong
// it should be considered a bug in IpsClient
@Suppress("UNCHECKED_CAST")
sealed class IpsResult<JId, WfId, Status, Custom> {
    data class Ok<JId, WfId, Status, Custom>(private val response: IpsResponse, private val data: Custom? = null) :
        IpsResult<JId, WfId, Status, Custom>() {
        val parts = response.parts
        val jobId = response.jobId as JId
        val workFlowId = response.workflowId as WfId
        val jobStatus = response.jobStatus as Status
        val customData: Custom = data as Custom
    }

    data class Error<JId, WfId, Status, Custom>(private val response: IpsResponse) :
        IpsResult<JId, WfId, Status, Custom>() {
        val parts = response.parts
        val jobId = response.jobId as JId
        val workflowId = response.workflowId as WfId
        val jobStatus = response.jobStatus as Status
    }

    data class Exception<JId, WfId, Status, Custom>(val throwable: IpsClientException) :
        IpsResult<JId, WfId, Status, Custom>() {
        override fun toString(): String {
            return this.throwable.stackTraceToString()
        }
    }

    fun throwIfNotOk(customMessage: String? = null): Ok<JId, WfId, Status, Custom> {
        if (this !is Ok) {
            throw IpsClientException("${customMessage ?: "IpsResult is not Ok"} - $this")
        }
        return this
    }

    fun isOk() = this is Ok
    fun isNotOk() = this !is Ok

    inline fun ifOk(block: (Ok<JId, WfId, Status, Custom>) -> Unit) = this.also {
        if (this is Ok) {
            block(this)
        }
    }

    inline fun ifNotException(block: (Ok<JId, WfId, Status, Custom>) -> Unit) = this.also {
        if (this is Ok) {
            block(this)
        }
    }

//    fun ifNotException(block: (Error<JId, WfId, Status, Custom>) -> Unit) = this.also {
//        if (this is Error) {
//            block(this)
//        }
//    }

    inline fun ifNotSuccess(block: (IpsResult<JId, WfId, Status, Custom>) -> Unit) = this.also {
        if (this is Error || this is Exception) {
            block(this)
        }
    }

}

data class IpsResponse(private val response: String) {
    val parts = response.split(";").filter { it.isNotBlank() }
    val status: IpsResponseStatus
        get() = when (parts[0]) {
            "ok" -> IpsResponseStatus.Ok
            "error" -> IpsResponseStatus.Error
            else -> throw IpsClientException("Invalid response status: '${parts[0]}'")
        }

    val jobId = parts.getOrNull(1)?.toIntOrNull()?.run { JobId(this) }
    val workflowId = parts.getOrNull(2)?.toIntOrNull()?.run { WorkFlowId(this) }
    val jobStatus = parts.getOrNull(5)?.let { JobStatus.valueOf(it) }
}

@JvmInline
value class JobId(val value: Int) {
    override fun toString() = value.toString()
}

@JvmInline
value class WorkFlowId(val value: Int) {
    override fun toString() = value.toString()
}

open class IpsClientException(message: String, cause: Throwable? = null) : Exception(message, cause)
class IpsFailedWriteException(command: String, cause: Throwable? = null) :
    IpsClientException("Failed to send IPS command. Command: '$command'", cause)

fun <JId, WfId, Status, Custom> IpsClientException.toIpsResult() = IpsResult.Exception<JId, WfId, Status, Custom>(this)
fun <JId, WfId, Status, Custom> IpsResponse.toIpsResult() = when (status) {
    IpsResponseStatus.Ok -> IpsResult.Ok<JId, WfId, Status, Custom>(this)
    IpsResponseStatus.Error -> IpsResult.Error<JId, WfId, Status, Custom>(this)
}

enum class JobStatus {
    Working, Finished, Aborted, Failed
}

enum class WaitForJobResult {
    Finished, Expired, Unknown
}
