@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.quadient.migration.service

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ScriptJobService {
    private val jobs = ConcurrentHashMap<JobId, Job>()

    fun create(path: String): Job.Running {
        return Job.Running(JobId(Uuid.random()), path, mutableListOf(), Clock.System.now()).also { jobs[it.id] = it }
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
    val logs: MutableList<String>
    val path: String
    val lastUpdated: Instant

    fun appendLog(message: String) {
        this.logs += message
    }

    fun appendLogs(logs: Iterable<String>) {
        this.logs.addAll(logs)
    }

    data class Running(
        override val id: JobId,
        override val path: String,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job {
        fun error(error: String): Error {
            return Error(id, path, logs, error, Clock.System.now())
        }

        fun success(): Success {
            return Success(id, path, logs, Clock.System.now())
        }
    }

    data class Success(
        override val id: JobId,
        override val path: String,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job

    data class Error(
        override val id: JobId,
        override val path: String,
        override val logs: MutableList<String>,
        val error: String,
        override val lastUpdated: Instant
    ) : Job
}

@JvmInline
value class JobId(val id: Uuid) {
    override fun toString(): String = id.toString()
}