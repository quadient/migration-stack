package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.table.FileTable
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

data class File @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var targetFolder: String?,
    var fileType: FileType,
    val skip: SkipOptions,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return emptyList()
    }

    companion object {
        fun fromDb(row: ResultRow): File {
            return File(
                id = row[FileTable.id].value,
                name = row[FileTable.name],
                originLocations = row[FileTable.originLocations],
                customFields = CustomFieldMap(row[FileTable.customFields].toMutableMap()),
                created = row[FileTable.created],
                lastUpdated = row[FileTable.created],
                sourcePath = row[FileTable.sourcePath],
                targetFolder = row[FileTable.targetFolder]?.let(IcmPath::from)?.toString(),
                fileType = FileType.valueOf(row[FileTable.fileType]),
                skip = row[FileTable.skip],
            )
        }
    }
}
