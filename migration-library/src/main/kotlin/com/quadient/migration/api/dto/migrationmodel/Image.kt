package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.ImageModel
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions

data class Image(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var options: ImageOptions?,
    var imageType: ImageType?,
    var targetFolder: String?,
    val metadata: Map<String, List<MetadataPrimitive>>,
    val skip: SkipOptions,
) : MigrationObject {
    companion object {
        fun fromModel(model: ImageModel): Image {
            return Image(
                id = model.id,
                name = model.name,
                originLocations = model.originLocations,
                customFields = CustomFieldMap(model.customFields.toMutableMap()),
                sourcePath = model.sourcePath,
                options = model.options,
                imageType = model.imageType,
                targetFolder = model.targetFolder?.toString(),
                metadata = model.metadata,
                skip = model.skip,
            )
        }
    }
}