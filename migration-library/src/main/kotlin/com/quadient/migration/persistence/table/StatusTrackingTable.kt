package com.quadient.migration.persistence.table

import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.service.deploy.ResourceType
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.json.jsonb

object StatusTrackingTable : CompositeIdTable("status_tracking") {
    val resourceId = varchar("id", 255).entityId()
    val resourceType = varchar("resource_type", 255).entityId()
    val projectName = varchar("project_name", 50).entityId()
    val status_events = jsonb<List<StatusEvent>>("status_events", Json)

    override val primaryKey = PrimaryKey(resourceId, resourceType, projectName)
}

class StatusTrackingEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<StatusTrackingEntity>(StatusTrackingTable)

    val resourceId by StatusTrackingTable.resourceId
    val resourceType by StatusTrackingTable.resourceType
    val projectName by StatusTrackingTable.projectName
    var statusEvents by StatusTrackingTable.status_events

    fun toDto(): StatusTracking {
        return StatusTracking(
            id = resourceId.value,
            resourceType = ResourceType.valueOf(resourceType.value),
            projectName = projectName.value,
            statusEvents = statusEvents
        )
    }
}