@file:OptIn(ExperimentalTime::class)

package com.quadient.migration.dto

import com.quadient.migration.service.Job
import com.quadient.migration.service.JobId
import com.quadient.migration.service.ScriptId
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class JobResponse(
    val id: JobId,
    val moduleId: ScriptId,
    val status: Status,
    val lastUpdated: String,
    val logs: List<String>?,
    val error: String?
)

@Serializable
data class JobListResponse(
    val id: JobId,
    val moduleId: ScriptId,
    val status: Status,
    val lastUpdated: String,
    val error: String?
)

fun Job.toResponse(): JobResponse {
    return when (this) {
        is Job.Running -> JobResponse(id, scriptId, Status.RUNNING, lastUpdated.toString(), logs, null)
        is Job.Success -> JobResponse(id, scriptId, Status.SUCCESS, lastUpdated.toString(), logs, null)
        is Job.Error -> JobResponse(id, scriptId, Status.ERROR, lastUpdated.toString(), logs, error)
    }
}

fun Job.toResponseWithoutLogs(): JobListResponse {
    return when (this) {
        is Job.Running -> JobListResponse(id, scriptId, Status.RUNNING, lastUpdated.toString(), null)
        is Job.Success -> JobListResponse(id, scriptId, Status.SUCCESS, lastUpdated.toString(), null)
        is Job.Error -> JobListResponse(id, scriptId, Status.ERROR, lastUpdated.toString(), error)
    }
}

@Serializable
enum class Status {
    RUNNING, SUCCESS, ERROR
}
