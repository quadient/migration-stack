package com.quadient.migration.persistence.repository

import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.DocumentObjectTable.baseTemplate
import com.quadient.migration.persistence.table.DocumentObjectTable.content
import com.quadient.migration.persistence.table.DocumentObjectTable.displayRuleRef
import com.quadient.migration.persistence.table.DocumentObjectTable.internal
import com.quadient.migration.persistence.table.DocumentObjectTable.options
import com.quadient.migration.persistence.table.DocumentObjectTable.targetFolder
import com.quadient.migration.persistence.table.DocumentObjectTable.type
import com.quadient.migration.shared.DocumentObjectType
import org.jetbrains.exposed.sql.ResultRow

class DocumentObjectInternalRepository(table: DocumentObjectTable, projectName: String) :
    InternalRepository<DocumentObjectModel>(table, projectName) {
    override fun toModel(row: ResultRow): DocumentObjectModel {
        return DocumentObjectModel(
            id = row[table.id].value,
            name = row[table.name],
            type = DocumentObjectType.valueOf(row[type]),
            content = row[content]?.map(DocumentContentModel::fromDbContent) ?: emptyList(),
            internal = row[internal],
            targetFolder = row[targetFolder],
            originLocations = row[table.originLocations],
            customFields = row[table.customFields],
            created = row[table.created],
            lastUpdated = row[table.lastUpdated],
            baseTemplate = row[baseTemplate],
            displayRuleRef = row[displayRuleRef]?.let { DisplayRuleModelRef(it) },
            options = row[options]
        )
    }
}