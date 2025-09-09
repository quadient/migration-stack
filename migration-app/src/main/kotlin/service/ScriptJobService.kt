@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.quadient.migration.log
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.uuid.ExperimentalUuidApi

typealias Jobs = ConcurrentHashMap<String, ConcurrentHashMap<JobId, Job>>

class ScriptJobService(val settings: SettingsService, val fileStorageService: FileStorageService) {
    private val jobs: Jobs = readAllJobs()
    private val projectJobs: ConcurrentHashMap<JobId, Job>
        get() = jobs.getOrPut(settings.activeProject) { ConcurrentHashMap() }

    private val runningJobs: Jobs = ConcurrentHashMap()
    private val runningProjectJobs: ConcurrentHashMap<JobId, Job>
        get() = runningJobs.getOrPut(settings.activeProject) { ConcurrentHashMap() }

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            runningJobs.forEach { (project, jobsMap) ->
                jobsMap.values.filterIsInstance<Job.Running>().forEach { running ->
                    val errorJob = running.error("Job failed because server was shut down")
                    log.warn("Server is shutting down, marking job ${running.id} as error")
                    fileStorageService.writeAppJson(errorJob, project, "jobs", "${errorJob.id}.json")
                }
            }
        })
    }

    fun create(id: ScriptId): Job.Running {
        return Job.Running(JobId(UUID.randomUUID()), id, mutableListOf(), Instant.now()).also { store(it) }
    }

    fun store(job: Job) {
        projectJobs[job.id] = job
        when (job) {
            is Job.Running -> runningProjectJobs[job.id] = job
            else -> runningProjectJobs.remove(job.id)
        }
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
                    .mapNotNull {
                        val job = fileStorageService.readAppJson<Job>(projectDir.name, "jobs", it.name)
                        if (job is Job.Running) {
                            val errorJob = job.error("Job failed because the server was shut down")
                            log.warn("Found running job ${job.id} in project ${projectDir.name} during startup, marking as error")
                            fileStorageService.writeAppJson(errorJob, settings.activeProject, "jobs", "${job.id}.json")
                            errorJob
                        } else {
                            job
                        }
                    }
                    .associateBy { it.id }
            result[projectDir.name] = ConcurrentHashMap(projectJobs)
        }

        return result
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Job.Running::class, name = "Running"),
    JsonSubTypes.Type(value = Job.Success::class, name = "Success"),
    JsonSubTypes.Type(value = Job.Error::class, name = "Error"),
)
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

    data class Running @JsonCreator constructor(
        override val id: JobId,
        override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job {
        fun error(error: String): Error {
            return Error(id, scriptId, logs, error, Instant.now())
        }

        fun success(): Success {
            return Success(id, scriptId, logs, Instant.now())
        }
    }

    data class Success @JsonCreator constructor(
        override val id: JobId,
        override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        override val lastUpdated: Instant
    ) : Job

    data class Error @JsonCreator constructor(
        override val id: JobId,
        override val scriptId: ScriptId,
        override val logs: MutableList<String>,
        val error: String,
        override val lastUpdated: Instant
    ) : Job
}

@JvmInline
value class JobId @JsonCreator constructor(val id: UUID) {
    @JsonValue
    override fun toString(): String = id.toString()
}