package com.quadient.migration.persistence.table

import com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object TextStyleTable : MigrationObjectTable("text_style") {
    val definition = jsonb<TextStyleDefinitionEntity>("definition", Json)
    val targetId = varchar("target_id", 255).nullable()
}
