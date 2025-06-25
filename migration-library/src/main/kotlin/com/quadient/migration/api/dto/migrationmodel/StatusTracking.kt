package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.StatusEvent
import com.quadient.migration.service.deploy.ResourceType

data class StatusTracking(
    val id: String,
    val projectName: String,
    val resourceType: ResourceType,
    val statusEvents: List<StatusEvent>,
)
