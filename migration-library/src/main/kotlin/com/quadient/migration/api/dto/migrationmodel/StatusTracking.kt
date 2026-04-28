package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.StatusEvent
import com.quadient.migration.service.deploy.utility.ResourceType

data class ResourceId(val id: String, val type: ResourceType)
data class StatusTracking(
    val id: String,
    val projectName: String,
    val resourceType: ResourceType,
    val statusEvents: List<StatusEvent>,
) {
    fun resourceId() = ResourceId(id, resourceType)
}
