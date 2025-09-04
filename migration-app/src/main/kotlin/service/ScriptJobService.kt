@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

typealias Jobs = ConcurrentHashMap<String, ConcurrentHashMap<JobId, Job>>

class ScriptJobService(val settings: SettingsService, val fileStorageService: FileStorageService) {
    private val jobs: Jobs = readAllJobs()
    private val projectJobs: ConcurrentHashMap<JobId, Job>
        get() = jobs.getOrPut(settings.activeProject) { ConcurrentHashMap() }

    fun create(id: ScriptId): Job.Running {
        return Job.Running(JobId(Uuid.random()), id, mutableListOf(), Clock.System.now()).also { store(it) }
    }

    fun store(job: Job) {
        projectJobs[job.id] = job
        fileStorageService.writeAppJson(job, settings.activeProject, "jobs", "${job.id}.json")
    }

    fun get(id: JobId): Job? {
        return projectJobs[id]
    }

    fun list(): List<Job> {
        return projectJobs.values.toList()
    }

    fun readAllJobs(): Jobs {
        val projects = fileStorageService.list(StorageType.App).filter { it.isDirectory() }

        val result: Jobs = ConcurrentHashMap()
        for (projectDir in projects) {
            val projectJobs =
                fileStorageService.list(StorageType.App, projectDir.name, "jobs").filter { it.isRegularFile() }
                    .mapNotNull { fileStorageService.readAppJson<Job>(projectDir.name, "jobs", it.name) }
                    .associateBy { it.id }
            result[projectDir.name] = ConcurrentHashMap(projectJobs)
        }

        return result
    }
}

@Serializable
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

    @Serializable
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

    @Serializable
    data class Success(
        override val id: JobId, override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job

    @Serializable
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