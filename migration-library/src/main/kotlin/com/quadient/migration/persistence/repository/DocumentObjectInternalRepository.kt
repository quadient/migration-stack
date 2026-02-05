package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.persistence.table.DocumentObjectTable
import org.jetbrains.exposed.v1.core.ResultRow

class DocumentObjectInternalRepository(table: DocumentObjectTable, projectName: String) :
    InternalRepository<DocumentObject>(table, projectName) {
    override fun toModel(row: ResultRow): DocumentObject {
        return DocumentObjectTable.fromResultRow(row)
    }
}