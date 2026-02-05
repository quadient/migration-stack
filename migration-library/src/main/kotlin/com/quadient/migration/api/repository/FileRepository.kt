package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.persistence.repository.FileInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.FileTable
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.tools.concat
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning

class FileRepository(internalRepository: FileInternalRepository) : Repository<File>(internalRepository) {
    val statusTrackingRepository = StatusTrackingRepository(internalRepository.projectName)

    override fun toDto(model: File): File {
        return model
    }

    override fun findUsages(id: String): List<MigrationObject> {
        return transaction {
            DocumentObjectTable.selectAll().where { DocumentObjectTable.projectName eq internalRepository.projectName }
                .map { DocumentObjectTable.fromResultRow(it) }.filter { it.collectRefs().any { it.id == id } }
                .distinct()
        }
    }

    override fun upsert(dto: File) {
        internalRepository.upsert {
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.File)
            }

            internalRepository.table.upsertReturning(
                internalRepository.table.id, internalRepository.table.projectName
            ) {
                it[FileTable.id] = dto.id
                it[FileTable.projectName] = internalRepository.projectName
                it[FileTable.name] = dto.name
                it[FileTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
                it[FileTable.customFields] = dto.customFields.inner
                it[FileTable.created] = existingItem?.created ?: now
                it[FileTable.lastUpdated] = now
                it[FileTable.sourcePath] = dto.sourcePath
                it[FileTable.targetFolder] = dto.targetFolder
                it[FileTable.fileType] = dto.fileType.name
                it[FileTable.skip] = dto.skip
            }.first()
        }
    }

    override fun upsertBatch(dtos: Collection<File>) {
        internalRepository.upsertBatch(dtos) { dto ->
            val existingItem =
                internalRepository.table.selectAll().where(internalRepository.filter(dto.id)).firstOrNull()
                    ?.let { internalRepository.toModel(it) }

            val now = Clock.System.now()

            if (existingItem == null) {
                statusTrackingRepository.active(dto.id, ResourceType.File)
            }

            this[FileTable.id] = dto.id
            this[FileTable.projectName] = internalRepository.projectName
            this[FileTable.name] = dto.name
            this[FileTable.originLocations] = existingItem?.originLocations.concat(dto.originLocations).distinct()
            this[FileTable.customFields] = dto.customFields.inner
            this[FileTable.created] = existingItem?.created ?: now
            this[FileTable.lastUpdated] = now
            this[FileTable.sourcePath] = dto.sourcePath
            this[FileTable.targetFolder] = dto.targetFolder
            this[FileTable.fileType] = dto.fileType.name
            this[FileTable.skip] = dto.skip
        }
    }
}
