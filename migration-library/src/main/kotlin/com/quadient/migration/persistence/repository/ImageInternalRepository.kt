package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.persistence.table.MigrationObjectTable
import org.jetbrains.exposed.v1.core.ResultRow

class ImageInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<Image>(table, projectName) {
    override fun toModel(row: ResultRow): Image {
        return Image.fromDb(row)
    }
}