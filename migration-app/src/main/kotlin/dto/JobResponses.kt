@file:OptIn(ExperimentalTime::class)

package com.quadient.migration.dto

import com.quadient.migration.service.Job
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class JobResponse(
    val id: String,
    val path: String,
    val status: Status,
    val lastUpdated: String,
    val logs: List<String>?,
    val error: String?
)

@Serializable
data class JobListResponse(
    val id: String,
    val path: String,
    val status: Status,
    val lastUpdated: String,
    val error: String?
)

fun Job.toResponse(): JobResponse {
    val id = this.id.toString()
    return when (this) {
        is Job.Running -> JobResponse(id, path, Status.RUNNING, lastUpdated.toString(), logs, null)
        is Job.Success -> JobResponse(id, path, Status.SUCCESS, lastUpdated.toString(), logs, null)
        is Job.Error -> JobResponse(id, path, Status.ERROR, lastUpdated.toString(), logs, error)
    }
}

fun Job.toResponseWithoutLogs(): JobListResponse {
    val id = this.id.toString()
    return when (this) {
        is Job.Running -> JobListResponse(id, path, Status.RUNNING, lastUpdated.toString(), null)
        is Job.Success -> JobListResponse(id, path, Status.SUCCESS, lastUpdated.toString(), null)
        is Job.Error -> JobListResponse(id, path, Status.ERROR, lastUpdated.toString(), error)
    }
}

@Serializable
enum class Status {
    RUNNING, SUCCESS, ERROR
}
