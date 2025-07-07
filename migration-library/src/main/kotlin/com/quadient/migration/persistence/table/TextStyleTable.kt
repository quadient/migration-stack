package com.quadient.migration.persistence.table

import com.quadient.migration.persistence.migrationmodel.TextStyleDefOrRefEntity
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object TextStyleTable : MigrationObjectTable("text_style") {
    val definition = jsonb<TextStyleDefOrRefEntity>("definition", Json)
}