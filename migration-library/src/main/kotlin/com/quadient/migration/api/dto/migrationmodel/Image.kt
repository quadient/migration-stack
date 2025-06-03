package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType

data class Image(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var options: ImageOptions?,
    var imageType: ImageType?,
    var targetFolder: String?,
) : MigrationObject