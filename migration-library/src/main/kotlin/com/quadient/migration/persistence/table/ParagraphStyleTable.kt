package com.quadient.migration.persistence.table

import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefOrRefEntity
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object ParagraphStyleTable : MigrationObjectTable("paragraph_style") {
    val definition = jsonb<ParagraphStyleDefOrRefEntity>("definition", Json)
}
