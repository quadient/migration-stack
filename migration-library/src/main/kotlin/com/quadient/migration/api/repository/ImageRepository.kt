package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class ImageRepository(table: ImageTable, projectName: String) : Repository<Image>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)
    override fun fromDb(row: ResultRow): Image {
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

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: Image) {
        upsertInternal {
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.Image)
            }

            table.upsertReturning(table.id, table.projectName) {
                it[ImageTable.id] = dto.id
                it[ImageTable.projectName] = this@ImageRepository.projectName
                it[ImageTable.name] = dto.name
                it[ImageTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[ImageTable.customFields] = dto.customFields.inner
                it[ImageTable.created] = existingItem?.created ?: now
                it[ImageTable.lastUpdated] = now
                it[ImageTable.sourcePath] = dto.sourcePath
                it[ImageTable.options] = dto.options
                it[ImageTable.imageType] = dto.imageType?.toString() ?: ImageType.Unknown.toString()
                it[ImageTable.targetFolder] = dto.targetFolder
                it[ImageTable.metadata] = dto.metadata
                it[ImageTable.skip] = dto.skip
                it[ImageTable.alternateText] = dto.alternateText
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<Image>) {
        if (dtos.isEmpty()) return

        val columns = listOf(
            "id", "project_name", "name", "origin_locations", "custom_fields",
            "created", "last_updated", "source_path", "image_type", "options",
            "target_folder", "metadata", "skip", "alternate_text"
        )
        val sql = createSql(columns, dtos.size)
        val now = Clock.System.now()

        upsertBatchInternal(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = find(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.Image)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, this@ImageRepository.projectName)
                stmt.setString(index++, dto.name)
                stmt.setArray(index++, it.createArrayOf("text", existingItem?.originLocations.concat(dto.originLocations).distinct().toTypedArray()))
                stmt.setObject(index++, Json.encodeToString(dto.customFields.inner), Types.OTHER)
                stmt.setTimestamp(index++, java.sql.Timestamp.from((existingItem?.created ?: now).toJavaInstant()))
                stmt.setTimestamp(index++, java.sql.Timestamp.from(now.toJavaInstant()))
                stmt.setString(index++, dto.sourcePath)
                stmt.setString(index++, dto.imageType?.toString() ?: ImageType.Unknown.toString())
                stmt.setObject(index++, dto.options?.let { Json.encodeToString(it) }, Types.OTHER)
                stmt.setString(index++, dto.targetFolder)
                stmt.setObject(index++, Json.encodeToString(dto.metadata), Types.OTHER)
                stmt.setObject(index++, Json.encodeToString(dto.skip), Types.OTHER)
                stmt.setString(index++, dto.alternateText)
            }

            stmt.executeUpdate()
        }
    }
}