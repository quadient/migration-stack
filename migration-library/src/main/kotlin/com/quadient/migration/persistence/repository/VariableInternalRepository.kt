package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.persistence.table.MigrationObjectTable
import org.jetbrains.exposed.v1.core.ResultRow

class VariableInternalRepository(table: MigrationObjectTable, projectName: String) :
    InternalRepository<Variable>(table, projectName) {

    override fun toModel(row: ResultRow): Variable {
        return Variable.fromDb(row)
    }
}