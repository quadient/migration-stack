package com.quadient.migration.persistence.table

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object ImageTable : MigrationObjectTable("image") {
    val sourcePath = varchar("source_path", 255).nullable()
    val imageType = varchar("image_type", 50)
    val options = jsonb<ImageOptions>("options", Json).nullable()
    val targetFolder = varchar("target_folder", 255).nullable()
    val metadata = jsonb<Map<String, List<MetadataPrimitive>>>("metadata", Json)
    val skip = jsonb<SkipOptions>("skip", Json)
    val alternateText = varchar("alternate_text", 255).nullable()
    val targetAttachmentId = varchar("target_attachment_id", 255).nullable()
}