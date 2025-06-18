@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.api.repository

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.data.Error
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.persistence.table.StatusTrackingEntity
import com.quadient.migration.persistence.table.StatusTrackingTable
import com.quadient.migration.service.deploy.ResourceType
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StatusTrackingRepository(val projectName: String) {
    fun listAll(): List<StatusTrackingEntity> {
        return transaction {
            StatusTrackingEntity.find { StatusTrackingTable.projectName eq projectName }.toList()
        }
    }

    fun find(id: String, resourceType: ResourceType): StatusTrackingEntity? {
        return transaction {
            StatusTrackingEntity.findById(CompositeID {
                it[StatusTrackingTable.projectName] = projectName
                it[StatusTrackingTable.resourceType] = resourceType.name
                it[StatusTrackingTable.resourceId] = id
            })
        }
    }

    fun findLastEvent(id: String, resourceType: ResourceType): StatusEvent? {
        return find(id, resourceType)?.statusEvents?.lastOrNull()
    }

    fun findLastEventRelevantToOutput(id: String, resourceType: ResourceType, output: InspireOutput): StatusEvent? {
        return transaction {
            find(id, resourceType)?.statusEvents?.lastOrNull {
                when (it) {
                    is Active -> true
                    is Deployed -> it.output == output
                    is Error -> it.output == output
                }
            }
        }
    }

    fun active(id: String, resourceType: ResourceType, data: Map<String, String> = emptyMap()): StatusTrackingEntity {
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
    ): StatusTrackingEntity {
        return upsert(id, resourceType, Error(deploymentId, timestamp, output, icmPath, message, data))
    }

    fun deployed(
        id: String,
        deploymentId: Uuid,
        timestamp: Instant,
        resourceType: ResourceType,
        icmPath: String?,
        output: InspireOutput,
        data: Map<String, String> = emptyMap(),
    ): StatusTrackingEntity {
        return upsert(id, resourceType, Deployed(deploymentId, timestamp, output, icmPath, data))
    }

    fun upsert(id: String, resourceType: ResourceType, event: StatusEvent): StatusTrackingEntity {
        return transaction {
            val id = CompositeID {
                it[StatusTrackingTable.projectName] = projectName
                it[StatusTrackingTable.resourceType] = resourceType.name
                it[StatusTrackingTable.resourceId] = id
            }

            StatusTrackingEntity.findByIdAndUpdate(id) {
                it.statusEvents = it.statusEvents + event
            } ?: StatusTrackingEntity.new(id) {
                this.statusEvents = listOf(event)
            }
        }
    }

    fun deleteAll() {
        return transaction {
            StatusTrackingEntity.find { StatusTrackingTable.projectName eq projectName }.forEach { it.delete() }
        }
    }
}