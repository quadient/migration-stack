package com.quadient.migration.persistence.repository

import com.quadient.migration.data.FileModel
import com.quadient.migration.persistence.table.FileTable
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.IcmPath
import org.jetbrains.exposed.v1.core.ResultRow

class FileInternalRepository(
    table: MigrationObjectTable, projectName: String
) : InternalRepository<FileModel>(table, projectName) {
    override fun toModel(row: ResultRow): FileModel {
        return FileModel(
            id = row[table.id].value,
            name = row[table.name],
            originLocations = row[table.originLocations],
            customFields = row[table.customFields],
            created = row[table.created],
            sourcePath = row[FileTable.sourcePath],
            targetFolder = row[FileTable.targetFolder]?.let(IcmPath::from),
            fileType = FileType.valueOf(row[FileTable.fileType]),
            skip = row[FileTable.skip],
        )
    }
}
