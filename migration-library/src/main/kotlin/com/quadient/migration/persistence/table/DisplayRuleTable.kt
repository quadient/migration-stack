package com.quadient.migration.persistence.table

import com.quadient.migration.shared.DisplayRuleDefinition
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object DisplayRuleTable : MigrationObjectTable("display_rule") {
    val definition = jsonb<DisplayRuleDefinition>("definition", Json).nullable()
}
