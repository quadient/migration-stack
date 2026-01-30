package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.data.ImageModel
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import java.sql.Types
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class ImageRepository(internalRepository: ImageInternalRepository) : Repository<Image, ImageModel>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    override fun toDto(model: ImageModel): Image {
        return Image(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            sourcePath = model.sourcePath,
            options = model.options,
            imageType = model.imageType,
            targetFolder = model.targetFolder?.toString(),
            metadata = model.metadata,
            skip = model.skip,
            alternateText = model.alternateText,
        )
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .map { DocumentObject.fromModel(it) }.distinct()
        }
    }

    override fun upsert(dto: Image) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.Image)
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[ImageTable.id] = dto.id
                it[ImageTable.projectName] = internalRepository.projectName
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
        val sql = internalRepository.createSql(columns, dtos.size)
        val now = Clock.System.now()

        internalRepository.upsertBatch(dtos) {
            val stmt = it.prepareStatement(sql)
            var index = 1
            dtos.forEach { dto ->
                val existingItem = internalRepository.findModel(dto.id)

                if (existingItem == null) {
                    statusTrackingRepository.active(dto.id, ResourceType.Image)
                }

                stmt.setString(index++, dto.id)
                stmt.setString(index++, internalRepository.projectName)
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