package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import kotlinx.datetime.Instant

data class ImageModel(
    override val id: String,
    override val name: String?,
    override val originLocations: List<String>,
    override val customFields: Map<String, String>,
    override val created: Instant,
    val sourcePath: String?,
    val imageType: ImageType,
    val options: ImageOptions?,
    val targetFolder: IcmPath?,
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return emptyList()
    }
}