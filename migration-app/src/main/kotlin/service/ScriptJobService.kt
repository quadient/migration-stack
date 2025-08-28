@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.quadient.migration.service

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ScriptJobService {
    private val jobs = ConcurrentHashMap<JobId, Job>()

    fun create(id: ScriptId): Job.Running {
        return Job.Running(JobId(Uuid.random()), id, mutableListOf(), Clock.System.now()).also { jobs[it.id] = it }
    }

    fun store(job: Job) {
        jobs[job.id] = job
    }

    fun get(id: JobId): Job? {
        return jobs[id]
    }

    fun list(): List<Job> {
        return jobs.values.toList()
    }
}

sealed interface Job {
    val id: JobId
    val scriptId: ScriptId
    val logs: MutableList<String>
    val lastUpdated: Instant

    fun appendLog(message: String) {
        this.logs += message
    }

    fun appendLogs(logs: Iterable<String>) {
        this.logs.addAll(logs)
    }

    data class Running(
        override val id: JobId, override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job {
        fun error(error: String): Error {
            return Error(id, scriptId, logs, error, Clock.System.now())
        }

        fun success(): Success {
            return Success(id, scriptId, logs, Clock.System.now())
        }
    }

    data class Success(
        override val id: JobId, override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job

    data class Error(
        override val id: JobId, override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        val error: String,
        override val lastUpdated: Instant
    ) : Job
}

@JvmInline
@Serializable
value class JobId(val id: Uuid) {
    override fun toString(): String = id.toString()
}