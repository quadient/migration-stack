@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.api.repository

import StatusTrackingInternalRepository
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.data.Error
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.service.deploy.ResourceType
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StatusTrackingRepository(val projectName: String) {
    val internalRepository = StatusTrackingInternalRepository(projectName)

    fun listAll(): List<StatusTracking> {
        return internalRepository.listAll().map { it.toDto() }
    }

    fun find(id: String, resourceType: ResourceType): StatusTracking? {
        return internalRepository.find(id, resourceType)?.toDto()
    }

    fun findLastEvent(id: String, resourceType: ResourceType): StatusEvent? {
        return find(id, resourceType)?.statusEvents?.lastOrNull()
    }

    fun findEventsRelevantToOutput(id: String, resourceType: ResourceType, output: InspireOutput): List<StatusEvent> {
        return internalRepository.findEventsRelevantToOutput(id, resourceType, output)
    }

    fun findLastEventRelevantToOutput(id: String, resourceType: ResourceType, output: InspireOutput): StatusEvent? {
        return internalRepository.findLastEventRelevantToOutput(id, resourceType, output)
    }

    fun active(id: String, resourceType: ResourceType, data: Map<String, String> = emptyMap()): StatusTracking {
        return upsert(id, resourceType, Active(data = data))
    }

    fun error(
        id: String,
        deploymentId: Uuid,
        timestamp: Instant,
        resourceType: ResourceType,
        icmPath: String?,
        output: InspireOutput,
        message: String,
        data: Map<String, String> = emptyMap(),
    ): StatusTracking {
        return upsert(id, resourceType, Error(deploymentId, timestamp, output, icmPath, message, data))
    }

    fun deployed(
        id: String,
        deploymentId: String,
        timestamp: Long,
        resourceType: ResourceType,
        icmPath: String?,
        output: InspireOutput,
        data: Map<String, String> = emptyMap(),
    ): StatusTracking {
        return deployed(
            id,
            Uuid.parse(deploymentId),
            Instant.fromEpochMilliseconds(timestamp),
            resourceType,
            icmPath,
            output,
            data
        )
    }

    fun deployed(
        id: String,
        deploymentId: Uuid,
        timestamp: Instant,
        resourceType: ResourceType,
        icmPath: String?,
        output: InspireOutput,
        data: Map<String, String> = emptyMap(),
    ): StatusTracking {
        return upsert(id, resourceType, Deployed(deploymentId, timestamp, output, icmPath, data))
    }

    fun upsert(id: String, resourceType: ResourceType, event: StatusEvent): StatusTracking {
        return internalRepository.upsert(id, resourceType, event).toDto()
    }

    fun deleteAll() {
        return internalRepository.deleteAll()
    }
}