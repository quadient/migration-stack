package com.quadient.migration.persistence.table

import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.SkipOptions
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object AttachmentTable : MigrationObjectTable("attachment") {
    val sourcePath = varchar("source_path", 255).nullable()
    val targetFolder = varchar("target_folder", 255).nullable()
    val attachmentType = varchar("attachment_type", 50).default(AttachmentType.Attachment.name)
    val skip = jsonb<SkipOptions>("skip", Json)
    val targetImageId = varchar("target_image_id", 255).nullable()
}
