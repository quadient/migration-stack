package com.quadient.migration.persistence.repository

import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariablePath
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.persistence.table.VariableStructureTable.structure
import org.jetbrains.exposed.sql.ResultRow

class VariableStructureInternalRepository(table: VariableStructureTable, projectName: String) :
    InternalRepository<VariableStructureModel>(table, projectName) {
    override fun toModel(row: ResultRow): VariableStructureModel {
        return VariableStructureModel(
            id = row[table.id].value,
            name = row[table.name],
            customFields = row[table.customFields],
            lastUpdated = row[table.lastUpdated],
            created = row[table.created],
            structure = row[structure].map { (key, value) -> VariableModelRef(key) to VariablePath(value) }.toMap(),
            originLocations = row[table.originLocations]
        )
    }
}