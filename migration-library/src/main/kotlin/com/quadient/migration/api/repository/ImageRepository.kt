package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class ImageRepository(internalRepository: ImageInternalRepository) : Repository<Image>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    override fun toDto(model: Image): Image {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
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
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.Image)
            }

            this[ImageTable.id] = dto.id
            this[ImageTable.projectName] = internalRepository.projectName
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