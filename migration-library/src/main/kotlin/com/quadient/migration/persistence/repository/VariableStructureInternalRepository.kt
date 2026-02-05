package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.table.VariableStructureTable
import org.jetbrains.exposed.v1.core.ResultRow

class VariableStructureInternalRepository(table: VariableStructureTable, projectName: String) :
    InternalRepository<VariableStructure>(table, projectName) {
    override fun toModel(row: ResultRow): VariableStructure {
        return VariableStructure.fromDb(row)
    }
}