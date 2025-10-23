package com.quadient.migration.persistence.repository

import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.table.VariableStructureTable
import org.jetbrains.exposed.v1.core.ResultRow

class VariableStructureInternalRepository(table: VariableStructureTable, projectName: String) :
    InternalRepository<VariableStructureModel>(table, projectName) {
    override fun toModel(row: ResultRow): VariableStructureModel {
        return VariableStructureModel(
            id = row[table.id].value,
            name = row[table.name],
            customFields = row[table.customFields],
            lastUpdated = row[table.lastUpdated],
            created = row[table.created],
            structure = row[VariableStructureTable.structure].map { (key, value) -> VariableModelRef(key) to value }.toMap(),
            originLocations = row[table.originLocations],
            languageVariable = row[VariableStructureTable.languageVariable]?.let { VariableModelRef(it) }
        )
    }
}