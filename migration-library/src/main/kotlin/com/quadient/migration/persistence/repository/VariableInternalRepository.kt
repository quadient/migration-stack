package com.quadient.migration.persistence.repository

import com.quadient.migration.data.VariableModel
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.persistence.table.VariableTable.created
import com.quadient.migration.persistence.table.VariableTable.customFields
import com.quadient.migration.persistence.table.VariableTable.id
import com.quadient.migration.persistence.table.VariableTable.dataType
import com.quadient.migration.persistence.table.VariableTable.defaultValue
import com.quadient.migration.persistence.table.VariableTable.lastUpdated
import com.quadient.migration.persistence.table.VariableTable.name
import com.quadient.migration.persistence.table.VariableTable.originLocations
import com.quadient.migration.shared.DataType
import org.jetbrains.exposed.v1.core.ResultRow

class VariableInternalRepository(table: MigrationObjectTable, projectName: String) :
    InternalRepository<VariableModel>(table, projectName) {

    override fun toModel(row: ResultRow): VariableModel {
        return VariableModel(
            id = row[id].value,
            name = row[name],
            originLocations = row[originLocations],
            customFields = row[customFields],
            lastUpdated = row[lastUpdated],
            created = row[created],
            dataType = DataType.valueOf(row[dataType]),
            defaultValue = row[defaultValue]
        )
    }
}