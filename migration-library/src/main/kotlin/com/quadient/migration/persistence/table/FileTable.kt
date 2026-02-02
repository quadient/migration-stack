package com.quadient.migration.persistence.table

import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.SkipOptions
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.jsonb

object FileTable : MigrationObjectTable("file") {
    val sourcePath = varchar("source_path", 255).nullable()
    val targetFolder = varchar("target_folder", 255).nullable()
    val fileType = varchar("file_type", 50).default(FileType.Document.name)
    val skip = jsonb<SkipOptions>("skip", Json)
}
