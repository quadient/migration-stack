package com.quadient.migration.persistence.table

import com.quadient.migration.shared.BaseTemplatePage
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object BaseTemplateTable : MigrationObjectTable("base_template") {
    val targetFolder = varchar("target_folder", 255).nullable()
    val pages = jsonb<List<BaseTemplatePage>>("pages", Json)
}
