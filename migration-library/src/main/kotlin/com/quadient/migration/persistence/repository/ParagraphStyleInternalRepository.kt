package com.quadient.migration.persistence.repository

import com.quadient.migration.data.ParagraphStyleDefOrRefModel
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.persistence.table.ParagraphStyleTable.definition
import org.jetbrains.exposed.v1.core.ResultRow

class ParagraphStyleInternalRepository(table: ParagraphStyleTable, projectName: String) :
    InternalRepository<ParagraphStyleModel>(table, projectName) {

    override fun toModel(row: ResultRow): ParagraphStyleModel {
        return ParagraphStyleModel(
            id = row[table.id].value,
            name = row[table.name],
            originLocations = row[table.originLocations],
            customFields = row[table.customFields],
            lastUpdated = row[table.lastUpdated],
            created = row[table.created],
            definition = ParagraphStyleDefOrRefModel.fromDb(row[definition]),
        )
    }

    internal fun firstWithDefinitionModel(id: String): ParagraphStyleModel? {
        val model = findModel(id)
        return when (model?.definition) {
            is ParagraphStyleDefinitionModel -> model
            is ParagraphStyleModelRef -> firstWithDefinitionModel(model.definition.id)
            null -> null
        }
    }
}