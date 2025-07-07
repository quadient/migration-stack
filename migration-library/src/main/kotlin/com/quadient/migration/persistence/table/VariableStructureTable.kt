package com.quadient.migration.persistence.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object VariableStructureTable : MigrationObjectTable("variable_structure") {
    val structure = jsonb<Map<String, String>>("structure", Json)
}
