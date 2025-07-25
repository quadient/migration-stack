package com.quadient.migration.persistence.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

abstract class MigrationObjectTable(name: String) : IdTable<String>(name) {
    override val id = varchar("id", 255).entityId()
    val projectName = varchar("project_name", 50)
    val name = varchar("name", 255).nullable()
    val originLocations = array<String>("origin_locations")
    val customFields = jsonb<Map<String, String>>("custom_fields", Json)
    val lastUpdated = timestamp("last_updated")
    val created = timestamp("created")

    override val primaryKey = PrimaryKey(id, projectName)
}