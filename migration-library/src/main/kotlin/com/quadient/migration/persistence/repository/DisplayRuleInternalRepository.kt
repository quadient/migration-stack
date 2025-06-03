package com.quadient.migration.persistence.repository

import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.persistence.table.DisplayRuleTable.created
import com.quadient.migration.persistence.table.DisplayRuleTable.customFields
import com.quadient.migration.persistence.table.DisplayRuleTable.definition
import com.quadient.migration.persistence.table.DisplayRuleTable.id
import com.quadient.migration.persistence.table.DisplayRuleTable.lastUpdated
import com.quadient.migration.persistence.table.DisplayRuleTable.name
import com.quadient.migration.persistence.table.DisplayRuleTable.originLocations
import com.quadient.migration.persistence.table.MigrationObjectTable
import org.jetbrains.exposed.sql.ResultRow

class DisplayRuleInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<DisplayRuleModel>(table, projectName) {
    override fun toModel(row: ResultRow): DisplayRuleModel {
        return DisplayRuleModel(
            id = row[id].value,
            name = row[name],
            originLocations = row[originLocations],
            customFields = row[customFields],
            lastUpdated = row[lastUpdated],
            created = row[created],
            definition = row[definition]
        )
    }
}
