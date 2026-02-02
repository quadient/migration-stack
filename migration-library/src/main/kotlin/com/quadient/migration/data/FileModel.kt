package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant

data class FileModel(
    override val id: String,
    override val name: String?,
    override val originLocations: List<String>,
    override val customFields: Map<String, String>,
    override val created: Instant,
    val sourcePath: String?,
    val targetFolder: IcmPath?,
    val fileType: FileType,
    val skip: SkipOptions,
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return emptyList()
    }
}
