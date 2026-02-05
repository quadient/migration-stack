package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.persistence.table.MigrationObjectTable
import org.jetbrains.exposed.v1.core.ResultRow

class DisplayRuleInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<DisplayRule>(table, projectName) {
    override fun toModel(row: ResultRow): DisplayRule {
        return DisplayRule.fromDb(row)
    }
}
