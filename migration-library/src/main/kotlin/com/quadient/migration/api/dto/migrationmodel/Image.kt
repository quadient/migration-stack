package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

data class Image @JvmOverloads constructor(
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
    var alternateText: String? = null,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return emptyList()
    }

    companion object {
        fun fromDb(row: ResultRow): Image {
            return Image(
                id = row[ImageTable.id].value,
                name = row[ImageTable.name],
                originLocations = row[ImageTable.originLocations],
                customFields = CustomFieldMap(row[ImageTable.customFields].toMutableMap()),
                created = row[ImageTable.created],
                lastUpdated = row[ImageTable.created],
                sourcePath = row[ImageTable.sourcePath],
                imageType = ImageType.valueOf(row[ImageTable.imageType]),
                options = row[ImageTable.options],
                targetFolder = row[ImageTable.targetFolder]?.let(IcmPath::from)?.toString(),
                metadata = row[ImageTable.metadata],
                skip = row[ImageTable.skip],
                alternateText = row[ImageTable.alternateText],
            )
        }
    }
}