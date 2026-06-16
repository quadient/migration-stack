package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataEntry
import com.quadient.migration.shared.SkipOptions
import kotlin.time.Instant

data class Image @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var options: ImageOptions?,
    var imageType: ImageType?,
    var targetFolder: String?,
    val metadata: List<MetadataEntry> = emptyList(),
    val skip: SkipOptions,
    var alternateText: String? = null,
    var targetAttachmentId: String? = null,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return listOfNotNull(targetAttachmentId?.let { AttachmentRef(it) })
    }
}