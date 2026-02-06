package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant

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
}