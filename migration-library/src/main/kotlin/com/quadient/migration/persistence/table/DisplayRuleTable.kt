package com.quadient.migration.persistence.table

import com.quadient.migration.persistence.migrationmodel.BaseTemplateLocationEntity
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.MetadataEntry
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object DisplayRuleTable : MigrationObjectTable("display_rule") {
    val definition = jsonb<DisplayRuleDefinition>("definition", Json).nullable()
    val targetId = varchar("target_id", 255).nullable()
    val internal = bool("internal")
    val subject = varchar("subject", 255).nullable()
    val targetFolder = varchar("target_folder", 255).nullable()
    val baseTemplate = jsonb<BaseTemplateLocationEntity>("base_template", Json).nullable()
    val variableStructureRef = varchar("variable_structure_ref", 255).nullable()
    val metadata = jsonb<List<MetadataEntry>>("metadata", Json)
}
