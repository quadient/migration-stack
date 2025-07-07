package com.quadient.migration.persistence.repository

import com.quadient.migration.data.TextStyleDefOrRefModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.persistence.table.TextStyleTable.definition
import org.jetbrains.exposed.v1.core.ResultRow

class TextStyleInternalRepository(table: TextStyleTable, projectName: String) :
    InternalRepository<TextStyleModel>(table, projectName) {

    override fun toModel(row: ResultRow): TextStyleModel {
        return TextStyleModel(
            id = row[table.id].value,
            name = row[table.name],
            originLocations = row[table.originLocations],
            customFields = row[table.customFields],
            lastUpdated = row[table.lastUpdated],
            created = row[table.created],
            definition = TextStyleDefOrRefModel.fromDb(row[definition]),
        )
    }

    internal fun firstWithDefinitionModel(id: String): TextStyleModel? {
        val model = findModel(id)
        return when (model?.definition) {
            is TextStyleDefinitionModel -> model
            is TextStyleModelRef -> firstWithDefinitionModel(model.definition.id)
            null -> null
        }
    }
}