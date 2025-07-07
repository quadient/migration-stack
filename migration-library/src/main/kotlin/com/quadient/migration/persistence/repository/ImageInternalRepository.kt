package com.quadient.migration.persistence.repository

import com.quadient.migration.data.ImageModel
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.shared.ImageType
import org.jetbrains.exposed.v1.core.ResultRow

class ImageInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<ImageModel>(table, projectName) {
    override fun toModel(row: ResultRow): ImageModel {
        return ImageModel(
            id = row[table.id].value,
            name = row[table.name],
            originLocations = row[table.originLocations],
            customFields = row[table.customFields],
            created = row[table.created],
            sourcePath = row[ImageTable.sourcePath],
            imageType = ImageType.valueOf(row[ImageTable.imageType]),
            options = row[ImageTable.options],
            targetFolder = row[ImageTable.targetFolder]
        )
    }
}