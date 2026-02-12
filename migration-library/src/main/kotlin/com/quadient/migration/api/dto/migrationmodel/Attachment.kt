package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant

data class Attachment @JvmOverloads constructor(
    override val id: String,
    override var name: String?,
    override var originLocations: List<String>,
    override var customFields: CustomFieldMap,
    var sourcePath: String?,
    var targetFolder: String?,
    var attachmentType: AttachmentType,
    val skip: SkipOptions,
    var targetImageId: String? = null,
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return listOfNotNull(targetImageId?.let { ImageRef(it) })
    }
}