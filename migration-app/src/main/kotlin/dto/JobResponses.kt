package com.quadient.migration.dto

import com.quadient.migration.service.Job
import com.quadient.migration.service.JobId
import com.quadient.migration.service.RunId
import com.quadient.migration.service.ScriptId

data class JobResponse(
    val id: JobId,
    val moduleId: ScriptId,
    val status: Status,
    val lastUpdated: String,
    val logs: List<String>?,
    val error: String?,
    val runId: RunId,
)

data class JobListResponse(
    val id: JobId,
    val moduleId: ScriptId,
    val status: Status,
    val lastUpdated: String,
    val error: String?,
    val runId: RunId,
)

fun Job.toResponse(): JobResponse {
    return when (this) {
        is Job.Running -> JobResponse(id, scriptId, Status.RUNNING, lastUpdated.toString(), logs, null, runId)
        is Job.Success -> JobResponse(id, scriptId, Status.SUCCESS, lastUpdated.toString(), logs, null, runId)
        is Job.Error -> JobResponse(id, scriptId, Status.ERROR, lastUpdated.toString(), logs, error, runId)
    }
}

fun Job.toResponseWithoutLogs(): JobListResponse {
    return when (this) {
        is Job.Running -> JobListResponse(id, scriptId, Status.RUNNING, lastUpdated.toString(), null, runId)
        is Job.Success -> JobListResponse(id, scriptId, Status.SUCCESS, lastUpdated.toString(), null, runId)
        is Job.Error -> JobListResponse(id, scriptId, Status.ERROR, lastUpdated.toString(), error, runId)
    }
}

enum class Status {
    RUNNING, SUCCESS, ERROR
}
