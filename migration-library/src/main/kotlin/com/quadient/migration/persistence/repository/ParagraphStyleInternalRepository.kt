package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.persistence.table.ParagraphStyleTable
import org.jetbrains.exposed.v1.core.ResultRow

class ParagraphStyleInternalRepository(table: ParagraphStyleTable, projectName: String) :
    InternalRepository<ParagraphStyle>(table, projectName) {

    override fun toModel(row: ResultRow): ParagraphStyle {
        return ParagraphStyle.fromDb(row)
    }

    internal fun firstWithDefinition(id: String): ParagraphStyle? {
        val model = findModel(id)
        val def = model?.definition
        return when (def) {
            is ParagraphStyleDefinition -> model
            is ParagraphStyleRef -> firstWithDefinition(def.id)
            null -> null
        }
    }
}