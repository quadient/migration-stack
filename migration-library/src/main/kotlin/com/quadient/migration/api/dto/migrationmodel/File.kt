package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.FileModel
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions

data class File(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var targetFolder: String?,
    var fileType: FileType,
    val skip: SkipOptions,
) : MigrationObject {
    companion object {
        fun fromModel(model: FileModel): File {
            return File(
                id = model.id,
                name = model.name,
                originLocations = model.originLocations,
                customFields = CustomFieldMap(model.customFields.toMutableMap()),
                sourcePath = model.sourcePath,
                targetFolder = model.targetFolder?.toString(),
                fileType = model.fileType,
                skip = model.skip,
            )
        }
    }
}
