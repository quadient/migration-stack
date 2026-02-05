package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.persistence.table.TextStyleTable
import org.jetbrains.exposed.v1.core.ResultRow

class TextStyleInternalRepository(table: TextStyleTable, projectName: String) :
    InternalRepository<TextStyle>(table, projectName) {

    override fun toModel(row: ResultRow): TextStyle {
        return TextStyle.fromDb(row)
    }

    internal fun firstWithDefinition(id: String): TextStyle? {
        val model = findModel(id)
        val def = model?.definition
        return when (def) {
            is TextStyleDefinition -> model
            is TextStyleRef -> firstWithDefinition(def.id)
            null -> null
        }
    }
}