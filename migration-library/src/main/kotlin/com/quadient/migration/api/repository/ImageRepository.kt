package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class ImageRepository(table: ImageTable, projectName: String) : Repository<Image>(table, projectName) {
    val statusTrackingRepository = StatusTrackingRepository(projectName)

    override fun fromDb(row: ResultRow): Image {
        return Image.fromDb(row)
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
                it[ImageTable.projectName] = projectName
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
        upsertBatchInternal(dtos) { dto ->
            val existingItem = table.selectAll().where(filter(dto.id)).firstOrNull()?.let(::fromDb)

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.Image)
            }

            this[ImageTable.id] = dto.id
            this[ImageTable.projectName] = projectName
            this[ImageTable.name] = dto.name
            this[ImageTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[ImageTable.customFields] = dto.customFields.inner
            this[ImageTable.created] = existingItem?.created ?: now
            this[ImageTable.lastUpdated] = now
            this[ImageTable.sourcePath] = dto.sourcePath
            this[ImageTable.options] = dto.options
            this[ImageTable.imageType] = dto.imageType?.toString() ?: ImageType.Unknown.toString()
            this[ImageTable.targetFolder] = dto.targetFolder
            this[ImageTable.metadata] = dto.metadata
            this[ImageTable.skip] = dto.skip
            this[ImageTable.alternateText] = dto.alternateText
        }
    }
}