package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.persistence.table.MigrationObjectTable
import org.jetbrains.exposed.v1.core.ResultRow

class FileInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<File>(table, projectName) {
    override fun toModel(row: ResultRow): File {
        return File.fromDb(row)
    }
}
