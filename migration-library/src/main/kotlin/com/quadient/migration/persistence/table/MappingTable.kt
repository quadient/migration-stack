package com.quadient.migration.persistence.table

import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.json.jsonb

object MappingTable : CompositeIdTable("mapping") {
    val resourceId = varchar("id", 255).entityId()
    val type = varchar("type", 255).entityId()
    val projectName = varchar("project_name", 50).entityId()
    val mapping = jsonb<MappingItemEntity>("mappings", Json)

    override val primaryKey = PrimaryKey(resourceId, type, projectName)
}