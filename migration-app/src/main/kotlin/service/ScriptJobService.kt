@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service

import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ScriptJobService {
    private val jobs = ConcurrentHashMap<JobId, Job>()

    fun create(path: String): Job.Running {
        return Job.Running(JobId(Uuid.random()), path, mutableListOf()).also { jobs[it.id] = it }
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

    fun appendLog(message: String) {
        this.logs += message
    }

    fun appendLogs(logs: Iterable<String>) {
        this.logs.addAll(logs)
    }

    data class Running(override val id: JobId, override val path: String, override val logs: MutableList<String>) :
        Job {
        fun error(error: String): Error {
            return Error(id, path, logs, error)
        }

        fun success(): Success {
            return Success(id, path, logs)
        }
    }

    data class Success(override val id: JobId, override val path: String, override val logs: MutableList<String>) : Job
    data class Error(
        override val id: JobId,
        override val path: String,
        override val logs: MutableList<String>,
        val error: String
    ) : Job
}

@JvmInline
value class JobId(val id: Uuid) {
    override fun toString(): String = id.toString()
}